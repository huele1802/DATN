package com.example.AI.Hotel.service;

import com.example.AI.Hotel.dto.HotelSearchRequest;
import com.example.AI.Hotel.dto.HotelSearchResponse;
import com.example.AI.Hotel.dto.NearByPlaceDto;
import com.example.AI.Hotel.dto.RoomTypeDTO;
import com.example.AI.Hotel.model.*;
import com.example.AI.Hotel.repository.*;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    private static final int MAX_HOTELS = 50;
    private static final String EMBEDDING_API_URL = "http://localhost:8000/embed";
    private static final double DEFAULT_MAX_DISTANCE_METERS = 5000; // Bán kính mặc định 5km

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

    @Transactional(readOnly = true)
    public List<NearByPlaceDto> findNearbyPlaces(Integer hotelId, Double maxDistance, Integer limit) {
        logger.info("Finding nearby places for hotel ID: {}", hotelId);

        // Kiểm tra xem khách sạn có tồn tại không
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isEmpty()) {
            logger.warn("Hotel not found for ID: {}", hotelId);
            throw new RuntimeException("Hotel not found with ID: " + hotelId);
        }

        // Nếu maxDistance không được cung cấp, sử dụng giá trị mặc định
        double effectiveMaxDistance = (maxDistance != null) ? maxDistance : DEFAULT_MAX_DISTANCE_METERS;
        // Nếu limit không được cung cấp, trả về tất cả địa điểm (dùng giá trị rất lớn)
        int effectiveLimit = (limit != null) ? limit : Integer.MAX_VALUE;

        // Tìm các địa điểm gần
        List<Object[]> nearbyPlaces = placeRepository.findNearbyPlaces(hotelId, effectiveMaxDistance, effectiveLimit);
        List<NearByPlaceDto> nearbyPlaceDTOs = new ArrayList<>();

        for (Object[] placeResult : nearbyPlaces) {
            Integer placeId = (Integer) placeResult[0];
            String title = (String) placeResult[1];
            Double distanceInMeters = (Double) placeResult[2];

            NearByPlaceDto placeDTO = new NearByPlaceDto();
            placeDTO.setPlaceId(placeId);
            placeDTO.setTitle(title);
            placeDTO.setDistanceInMeters(distanceInMeters);
            nearbyPlaceDTOs.add(placeDTO);
        }

        logger.info("Found {} nearby places for hotel ID: {}", nearbyPlaceDTOs.size(), hotelId);
        return nearbyPlaceDTOs;
    }

    @Transactional(readOnly = true)
    public List<HotelSearchResponse> searchHotelsByPriceAndGuests(Double maxPrice, Integer numberOfGuests) {
        logger.info("Searching hotels with maxPrice: {} and numberOfGuests: {}", maxPrice, numberOfGuests);

        // Kiểm tra tham số đầu vào
        if (maxPrice == null || maxPrice <= 0) {
            throw new IllegalArgumentException("maxPrice must be a positive value");
        }
        if (numberOfGuests == null || numberOfGuests <= 0) {
            throw new IllegalArgumentException("numberOfGuests must be a positive value");
        }

        // Tìm các phòng thỏa mãn điều kiện
        List<RoomType> matchingRooms = roomTypeRepository.findByPriceAndGuests(maxPrice, numberOfGuests);
        if (matchingRooms.isEmpty()) {
            logger.info("No rooms found matching the criteria: maxPrice = {}, numberOfGuests = {}", maxPrice, numberOfGuests);
            return List.of();
        }

        // Nhóm các phòng theo hotelId
        Map<Integer, List<RoomType>> roomsByHotel = new HashMap<>();
        for (RoomType room : matchingRooms) {
            Integer hotelId = room.getHotel().getId();
            roomsByHotel.computeIfAbsent(hotelId, k -> new ArrayList<>()).add(room);
        }

        // Lấy danh sách hotelId
        List<Integer> hotelIds = new ArrayList<>(roomsByHotel.keySet());

        // Tìm thông tin khách sạn
        List<Hotel> hotels = hotelRepository.findAllById(hotelIds);
        if (hotels.isEmpty()) {
            logger.warn("No hotels found for the matching rooms");
            return List.of();
        }

        // Tạo danh sách kết quả
        List<HotelSearchResponse> responses = new ArrayList<>();
        for (Hotel hotel : hotels) {
            HotelSearchResponse response = new HotelSearchResponse();
            response.setHotelId(hotel.getId());
            response.setName(hotel.getName());
            response.setDescription(hotel.getDescription());
            response.setFacilities(hotel.getFacilities());
            response.setReviews(hotel.getReviews());
            response.setRatingStars(hotel.getRatingStars());
            response.setAddress(hotel.getAddress());
            // Không gán similarityScore, để nó là null
//            response.setPlaces(List.of()); // Không lấy thông tin địa điểm gần

            // Thêm danh sách phòng phù hợp
            List<RoomType> hotelRooms = roomsByHotel.get(hotel.getId());
            List<RoomTypeDTO> roomDTOs = new ArrayList<>();
            for (RoomType room : hotelRooms) {
                RoomTypeDTO roomDTO = new RoomTypeDTO();
                roomDTO.setRoomId(room.getId());
                roomDTO.setName(room.getName());
                roomDTO.setNumberOfGuests(room.getNumberOfGuests());
                roomDTO.setPrice(room.getPrice());
                // Không gán roomSimilarityScore, để nó là null
                roomDTOs.add(roomDTO);
            }
            response.setRooms(roomDTOs);

            responses.add(response);
        }

        logger.info("Found {} hotels with matching rooms", responses.size());
        return responses;
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
//            response.setPlaces(List.of());

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
}