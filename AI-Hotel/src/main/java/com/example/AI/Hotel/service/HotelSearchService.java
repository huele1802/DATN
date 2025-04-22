package com.example.AI.Hotel.service;

import com.example.AI.Hotel.dto.HotelSearchRequest;
import com.example.AI.Hotel.dto.HotelSearchResponse;
import com.example.AI.Hotel.dto.RoomTypeDTO;
import com.example.AI.Hotel.model.*;
import com.example.AI.Hotel.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HotelSearchService {
    private static final Logger logger = LoggerFactory.getLogger(HotelSearchService.class);

    private static final double SIMILARITY_THRESHOLD = 0.05;
    private static final double SIMILARITY_THRESHOLD_ROOM = 0.05;
    private static final int MAX_PLACES = 5;
    private static final int MAX_HOTELS = 50;
    private static final String EMBEDDING_API_URL = "http://localhost:8000/embed";

    private final HotelRepository hotelRepository;
    private final RoomRepository roomTypeRepository;
    private final PlaceRepository placeRepository;
    private final HotelEmbeddingRepository hotelEmbeddingRepository;
    private final RoomEmbeddingRepository roomEmbeddingRepository;
    private final PlaceEmbeddingRepository placeEmbeddingRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public HotelSearchService(
            HotelRepository hotelRepository,
            RoomRepository roomTypeRepository,
            PlaceRepository placeRepository,
            HotelEmbeddingRepository hotelEmbeddingRepository,
            RoomEmbeddingRepository roomEmbeddingRepository,
            PlaceEmbeddingRepository placeEmbeddingRepository
    ) {
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.placeRepository = placeRepository;
        this.hotelEmbeddingRepository = hotelEmbeddingRepository;
        this.roomEmbeddingRepository = roomEmbeddingRepository;
        this.placeEmbeddingRepository = placeEmbeddingRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional(readOnly = true)
    public List<HotelSearchResponse> searchHotels(@Valid HotelSearchRequest request) {
        logger.info("Processing search query: {}", request.getQuery());

        try {
            // Gọi API Python để tạo embedding
            float[] queryEmbedding = getQueryEmbedding(request.getQuery());
            logger.debug("Query embedding created, length: {}", queryEmbedding.length);

            // Tìm các khách sạn phù hợp
            List<HotelSearchResponse> results = findMatchingHotels(queryEmbedding);

            // Sắp xếp theo độ tương đồng
            results.sort(Comparator.comparingDouble(HotelSearchResponse::getSimilarityScore).reversed());
            logger.info("Found {} matching hotels", results.size());
            return results;

        } catch (Exception e) {
            logger.error("Error processing search query: {}", request.getQuery(), e);
            throw new RuntimeException("Failed to process search query", e);
        }
    }

    private List<HotelSearchResponse> findMatchingHotels(float[] queryEmbedding) {
        List<HotelSearchResponse> responses = new ArrayList<>();

        // Chuyển queryEmbedding thành chuỗi
        String queryEmbeddingStr = arrayToString(queryEmbedding);
        double maxDistance = 1 - SIMILARITY_THRESHOLD;

        // Tìm khách sạn giống nhất bằng pgvector
        List<Object[]> similarHotels = hotelEmbeddingRepository.findTopSimilarHotels(
                queryEmbeddingStr, maxDistance, MAX_HOTELS);
        logger.info("Hotels above threshold ({}): {}", SIMILARITY_THRESHOLD, similarHotels.size());

        // Debug nếu không có khách sạn nào
        if (similarHotels.isEmpty()) {
            logger.warn("No hotels found above similarity threshold {}. Checking all hotels for debug...", SIMILARITY_THRESHOLD);
            List<Object[]> allHotels = hotelEmbeddingRepository.findTopSimilarHotels(queryEmbeddingStr, 2.0, 10);
            for (Object[] result : allHotels) {
                Integer hotelId = (Integer) result[0];
                double distance = (Double) result[1];
                double similarity = 1 - distance;
                logger.debug("Debug - Hotel ID: {}, Distance: {}, Similarity: {}", hotelId, distance, similarity);
            }
            return responses;
        }

        // Lấy danh sách hotelId đã chọn
        List<Integer> hotelIds = similarHotels.stream()
                .map(result -> (Integer) result[0])
                .collect(Collectors.toList());

        // Lấy RoomType cho các hotelId đã chọn
        List<RoomType> relevantRoomTypes = roomTypeRepository.findByHotelIdIn(hotelIds);
        Map<Integer, Integer> roomIdToHotelId = new HashMap<>();
        for (RoomType roomType : relevantRoomTypes) {
            roomIdToHotelId.put(roomType.getId(), roomType.getHotel().getId());
        }

        // Lấy RoomEmbedding cho các roomId đã chọn
        List<Integer> roomIds = relevantRoomTypes.stream()
                .map(RoomType::getId)
                .collect(Collectors.toList());
        List<RoomEmbedding> relevantRoomEmbeddings = roomIds.isEmpty() ?
                List.of() : roomEmbeddingRepository.findByRoomIdIn(roomIds);

        // Nhóm RoomEmbedding theo hotelId
        Map<Integer, List<RoomEmbedding>> roomEmbeddingsByHotel = new HashMap<>();
        for (RoomEmbedding roomEmbedding : relevantRoomEmbeddings) {
            Integer roomId = roomEmbedding.getRoomId();
            Integer hotelId = roomIdToHotelId.get(roomId);
            if (hotelId != null) {
                roomEmbeddingsByHotel.computeIfAbsent(hotelId, k -> new ArrayList<>()).add(roomEmbedding);
            }
        }

        // Xử lý từng khách sạn giống nhất
        for (Object[] result : similarHotels) {
            Integer hotelId = (Integer) result[0];
            double distance = (Double) result[1];
            double similarity = 1 - distance;
            logger.debug("Processing Hotel ID: {}, Distance: {}, Similarity: {}", hotelId, distance, similarity);

            // Lấy thông tin khách sạn
            Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
            if (hotelOpt.isEmpty()) {
                logger.warn("Hotel not found for ID: {}", hotelId);
                continue;
            }
            Hotel hotel = hotelOpt.get();

            // Tạo HotelSearchResponse
            HotelSearchResponse response = new HotelSearchResponse();
            response.setHotelId(hotelId);
            response.setName(hotel.getName());
            response.setDescription(hotel.getDescription());
            response.setFacilities(hotel.getFacilities());
            response.setReviews(hotel.getReviews());
            response.setRatingStars(hotel.getRatingStars());
            response.setAddress(hotel.getAddress());
            response.setSimilarityScore(similarity);

            // Thêm thông tin phòng
            List<RoomEmbedding> hotelRoomEmbeddings = roomEmbeddingsByHotel.getOrDefault(hotelId, List.of());
            List<RoomTypeDTO> rooms = new ArrayList<>();
            for (RoomEmbedding roomEmbedding : hotelRoomEmbeddings) {
                Optional<RoomType> roomTypeOpt = roomTypeRepository.findById(roomEmbedding.getRoomId());
                if (roomTypeOpt.isEmpty()) {
                    continue;
                }
                RoomType roomType = roomTypeOpt.get();

                float[] roomEmbeddingVector = parseEmbedding(roomEmbedding.getTextEmbedding());
                double roomSimilarity = calculateCosineSimilarity(queryEmbedding, roomEmbeddingVector);

                RoomTypeDTO roomDTO = new RoomTypeDTO();
                roomDTO.setRoomId(roomType.getId());
                roomDTO.setName(roomType.getName());
                roomDTO.setNumberOfGuests(roomType.getNumberOfGuests());
                roomDTO.setPrice(roomType.getPrice());
                roomDTO.setRoomSimilarityScore(roomSimilarity);
                rooms.add(roomDTO);
            }
            response.setRooms(rooms);

            // Bỏ qua places vì không có liên kết hotelId
            response.setPlaces(List.of());

            responses.add(response);
        }

        return responses;
    }

    private float[] getQueryEmbedding(String query) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("query", query);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    EMBEDDING_API_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Double> embeddingList = (List<Double>) response.getBody().get("embedding");
                if (embeddingList.size() != 768) {
                    throw new RuntimeException("Invalid embedding dimension: " + embeddingList.size());
                }
                float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    embedding[i] = embeddingList.get(i).floatValue();
                }
                return embedding;
            } else {
                throw new RuntimeException("Failed to get embedding from API: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error calling embedding API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get query embedding", e);
        }
    }

    private float[] parseEmbedding(String embeddingString) {
        try {
            String cleaned = embeddingString.replaceAll("[\\[\\]\\s]", "");
            String[] parts = cleaned.split(",");
            float[] embedding = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                embedding[i] = Float.parseFloat(parts[i]);
            }
            return embedding;
        } catch (Exception e) {
            logger.error("Failed to parse embedding: {}", embeddingString, e);
            throw new RuntimeException("Invalid embedding format", e);
        }
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
//    private String arrayToString(float[] array) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < array.length; i++) {
//            sb.append(array[i]);
//            if (i < array.length - 1) sb.append(",");
//        }
//        return sb.toString();
//    }
}