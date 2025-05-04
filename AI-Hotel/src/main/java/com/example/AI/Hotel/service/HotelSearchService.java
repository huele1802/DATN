package com.example.AI.Hotel.service;

import com.example.AI.Hotel.dto.*;
import com.example.AI.Hotel.model.*;
import com.example.AI.Hotel.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
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

    private static final double SIMILARITY_THRESHOLD = 0.68;
    private static final int MAX_HOTELS = 50;
    private static final String EMBEDDING_API_URL = "http://localhost:8000/embed";
    private static final double DEFAULT_MAX_DISTANCE_METERS = 5000; // Bán kính mặc định 5km

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final PlaceRepository placeRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public HotelSearchService(
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            PlaceRepository placeRepository,
            HotelEmbeddingRepository hotelEmbeddingRepository,
            PlaceEmbeddingRepository placeEmbeddingRepository,
            ObjectMapper objectMapper
    ) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.placeRepository = placeRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
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
    public List<HotelSearchResponse> searchHotelsV2(@Valid HotelSearchRequest request) {
        logger.info("Processing search query (v2): {}", request.getQuery());

        try {
            // Gọi API Python để tạo embedding
            float[] queryEmbedding = getQueryEmbedding(request.getQuery());
            logger.debug("Query embedding created, length: {}", queryEmbedding.length);

            // Tìm các khách sạn phù hợp
            List<HotelSearchResponse> results = findMatchingHotelsV2(queryEmbedding);

            // Sắp xếp theo độ tương đồng
            results.sort(Comparator.comparingDouble(HotelSearchResponse::getSimilarityScore).reversed());
            logger.info("Found {} matching hotels (v2)", results.size());
            return results;

        } catch (Exception e) {
            logger.error("Error processing search query (v2): {}", request.getQuery(), e);
            throw new RuntimeException("Failed to process search query (v2)", e);
        }
    }

    private List<HotelSearchResponse> findMatchingHotels(float[] queryEmbedding) {
        List<HotelSearchResponse> responses = new ArrayList<>();

        String queryEmbeddingStr = arrayToString(queryEmbedding);
//        double maxDistance = 1 - SIMILARITY_THRESHOLD;

        List<Object[]> similarHotels = hotelRepository.findTopSimilarHotels(
                queryEmbeddingStr, SIMILARITY_THRESHOLD, MAX_HOTELS);
        logger.info("Hotels above threshold ({}): {}", SIMILARITY_THRESHOLD, similarHotels.size());

        if (similarHotels.isEmpty()) {
            logger.warn("No hotels found above similarity threshold {}. Checking all hotels for debug...", SIMILARITY_THRESHOLD);
            List<Object[]> allHotels = hotelRepository.findTopSimilarHotels(queryEmbeddingStr, 0.0, 10);
            for (Object[] result : allHotels) {
                Integer hotelId = (Integer) result[0];
                double similarity = (Double) result[2];
                logger.debug("Debug - Hotel ID: {}, Similarity: {}", hotelId, similarity);
            }
            return responses;
        }

        List<Integer> hotelIds = similarHotels.stream()
                .map(result -> (Integer) result[0])
                .collect(Collectors.toList());

        List<RoomType> relevantRoomTypes = roomRepository.findByHotelIdIn(hotelIds);

        List<Hotel> hotels = hotelRepository.findAllById(hotelIds);
        for (Object[] result : similarHotels) {
            Integer hotelId = (Integer) result[0];
            double similarity = (Double) result[2];
            logger.debug("Processing Hotel ID: {}, Similarity: {}", hotelId, similarity);

            Optional<Hotel> hotelOpt = hotels.stream().filter(h -> h.getId().equals(hotelId)).findFirst();
            if (hotelOpt.isEmpty()) {
                logger.warn("Hotel not found for ID: {}", hotelId);
                continue;
            }
            Hotel hotel = hotelOpt.get();

            HotelSearchResponse response = new HotelSearchResponse();
            response.setHotel(toHotelDTO(hotel));
            response.setSimilarityScore(similarity);

            List<RoomType> hotelRooms = relevantRoomTypes.stream()
                    .filter(rt -> rt.getHotel().getId().equals(hotelId)).toList();
            List<RoomDTO> rooms = hotelRooms.stream()
                    .map(this::toRoomDTO)
                    .collect(Collectors.toList());
            response.setRooms(rooms);
//            response.setPlaces(List.of()); // Tạm thời để trống, có thể cập nhật sau

            responses.add(response);
        }
        return responses;
    }

    private List<HotelSearchResponse> findMatchingHotelsV2(float[] queryEmbedding) {
        List<HotelSearchResponse> responses = new ArrayList<>();

        String queryEmbeddingStr = arrayToString(queryEmbedding);
        logger.debug("Query embedding string: {}", queryEmbeddingStr); // Log để kiểm tra định dạng

        List<Object[]> similarHotels = hotelRepository.findTopSimilarHotels(
                queryEmbeddingStr, SIMILARITY_THRESHOLD, MAX_HOTELS);
        logger.info("Hotels above threshold (v2) ({}): {}", SIMILARITY_THRESHOLD, similarHotels.size());

        if (similarHotels.isEmpty()) {
            logger.warn("No hotels found above similarity threshold (v2) {}. Checking all hotels for debug...", SIMILARITY_THRESHOLD);
            List<Object[]> allHotels = hotelRepository.findTopSimilarHotelsForSearchV2(queryEmbeddingStr, 0.0, 10);
            for (Object[] result : allHotels) {
                Integer hotelId = (Integer) result[0];
                double similarity = (Double) result[2];
                logger.debug("Debug (v2) - Hotel ID: {}, Similarity: {}", hotelId, similarity);
            }
            return responses;
        }

        List<Integer> hotelIds = similarHotels.stream()
                .map(result -> (Integer) result[0])
                .collect(Collectors.toList());

        List<Hotel> hotels = hotelRepository.findAllById(hotelIds);
        for (Object[] result : similarHotels) {
            Integer hotelId = (Integer) result[0];
            double similarity = (Double) result[2];
            logger.debug("Processing Hotel ID (v2): {}, Similarity: {}", hotelId, similarity);

            Optional<Hotel> hotelOpt = hotels.stream().filter(h -> h.getId().equals(hotelId)).findFirst();
            if (hotelOpt.isEmpty()) {
                logger.warn("Hotel not found for ID (v2): {}", hotelId);
                continue;
            }
            Hotel hotel = hotelOpt.get();

            HotelSearchResponse response = new HotelSearchResponse();
            response.setHotel(toHotelDTO(hotel));
            response.setSimilarityScore(similarity);
//            response.setRooms(List.of()); // Để trống

            responses.add(response);
        }
        return responses;
    }
    @Transactional(readOnly = true)
    public List<PlaceDTO> findNearbyPlaces(Integer hotelId, Double maxDistance, Integer limit) {
        logger.info("Finding nearby places for hotel ID: {}", hotelId);

        // Kiểm tra xem khách sạn có tồn tại không
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isEmpty()) {
            logger.warn("Hotel not found for ID: {}", hotelId);
            throw new RuntimeException("Hotel not found with ID: " + hotelId);
        }

        // Nếu maxDistance không được cung cấp, sử dụng giá trị mặc định (5km)
        double effectiveMaxDistance = (maxDistance != null) ? maxDistance : DEFAULT_MAX_DISTANCE_METERS;
        // Nếu limit không được cung cấp, trả về tất cả địa điểm
        int effectiveLimit = (limit != null) ? limit : Integer.MAX_VALUE;

        // Tìm các địa điểm gần
        List<Object[]> nearbyPlaces = placeRepository.findNearbyPlaces(hotelId, effectiveMaxDistance, effectiveLimit);
        List<PlaceDTO> placeDTOs = new ArrayList<>();

        for (Object[] result : nearbyPlaces) {
            Object[] placeData = result;
            Double distanceInMeters = (Double) placeData[10]; // Vị trí mới của distance_in_meters

            // Log để kiểm tra kiểu dữ liệu
            for (int i = 0; i < placeData.length; i++) {
                logger.debug("Column {}: Type = {}, Value = {}", i, (placeData[i] != null ? placeData[i].getClass().getName() : "null"), placeData[i]);
            }

            // Ánh xạ từ Object[] sang Place
            Place place = new Place();
            place.setId(placeData[0] != null ? Integer.valueOf(placeData[0].toString()) : null);
            place.setTitle((String) placeData[1]);
            place.setRating(placeData[2] != null ? Float.parseFloat(placeData[2].toString()) : null);
            place.setAddress((String) placeData[3]);
            place.setReview(placeData[4] != null ? Integer.valueOf(placeData[4].toString()) : null);
            place.setSlug((String) placeData[5]);
            if (placeData[6] != null) {
                try {
                    String coordinatesStr = placeData[6].toString().trim();
                    if (!coordinatesStr.isEmpty()) {
                        GeometryFactory geometryFactory = new GeometryFactory();
                        WKTReader reader = new WKTReader(geometryFactory);
                        Point coordinates = (Point) reader.read(coordinatesStr);
                        place.setCoordinates(coordinates);
                    } else {
                        logger.warn("Empty coordinates for place ID {}", placeData[0]);
                        place.setCoordinates(null);
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse coordinates for place ID {}: {}", placeData[0], e.getMessage());
                    place.setCoordinates(null);
                }
            }
            place.setImageUrl((String) placeData[7]);
            place.setDescription((String) placeData[8]);
            if (placeData[9] != null) {
                try {
                    String servicesStr = placeData[9].toString().trim();
                    if (!servicesStr.isEmpty()) {
                        List<Map<String, List<String>>> services = objectMapper.readValue(
                                servicesStr, new TypeReference<List<Map<String, List<String>>>>() {});
                        place.setServices(services);
                    } else {
                        logger.warn("Empty services for place ID {}", placeData[0]);
                        place.setServices(null);
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse services for place ID {}: {}", placeData[0], e.getMessage());
                    place.setServices(null);
                }
            }

            // Chuyển đổi Place sang PlaceDTO
            PlaceDTO placeDTO = toPlaceDTO(place);
            placeDTO.setDistanceInMeters(distanceInMeters != null ? distanceInMeters : 0.0); // Gán giá trị mặc định nếu null

            placeDTOs.add(placeDTO);
        }

        logger.info("Found {} nearby places for hotel ID: {}", placeDTOs.size(), hotelId);
        return placeDTOs;
    }
    // Phương thức tính khoảng cách giữa hai điểm (latitude, longitude) bằng công thức Haversine
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Bán kính Trái Đất (mét)
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Khoảng cách tính bằng mét
    }

    // bắt buộc điền đầy đủ cả 3 trường
//    @Transactional(readOnly = true)
//    public List<HotelSearchResponse> searchHotelsByPriceGuestsAndDistrict(Integer maxPrice, Integer numberOfGuests, String district) {
//        logger.info("Searching hotels with maxPrice: {}, numberOfGuests: {}, district: {}", maxPrice, numberOfGuests, district);
//
//        // Kiểm tra maxPrice
//        if (maxPrice == null || maxPrice <= 0) {
//            throw new IllegalArgumentException("maxPrice must be a positive value");
//        }
//
//        // Kiểm tra numberOfGuests
//        if (numberOfGuests == null || numberOfGuests <= 0) {
//            throw new IllegalArgumentException("numberOfGuests must be a positive value");
//        }
//
//        // Kiểm tra district (cho phép null để không lọc theo district)
//        List<String> validDistricts = Arrays.asList("Liên Chiểu", "Hải Châu", "Thanh Khê", "Sơn Trà", "Ngũ Hành Sơn", "Cẩm Lệ");
//        if (district != null && !district.isEmpty() && !validDistricts.contains(district)) {
//            throw new IllegalArgumentException("District must be one of: " + validDistricts);
//        }
//
//        // Tìm các phòng thỏa mãn maxPrice và numberOfGuests
//        List<RoomType> matchingRooms = roomRepository.findByPriceAndGuests(Double.valueOf(maxPrice), numberOfGuests);
//        if (matchingRooms.isEmpty()) {
//            logger.info("No rooms found matching the criteria: maxPrice = {}, numberOfGuests = {}", maxPrice, numberOfGuests);
//            return List.of();
//        }
//
//        // Gom nhóm phòng theo hotelId
//        Map<Integer, List<RoomType>> roomsByHotel = new HashMap<>();
//        for (RoomType room : matchingRooms) {
//            Integer hotelId = room.getHotel().getId();
//            roomsByHotel.computeIfAbsent(hotelId, k -> new ArrayList<>()).add(room);
//        }
//
//        // Lấy danh sách hotelId
//        List<Integer> hotelIds = new ArrayList<>(roomsByHotel.keySet());
//
//        // Tìm khách sạn theo hotelIds và district (nếu có)
//        List<Hotel> hotels;
//        if (district != null && !district.isEmpty()) {
//            hotels = hotelRepository.findAllByIdAndDistrict(hotelIds, district);
//        } else {
//            hotels = hotelRepository.findAllById(hotelIds);
//        }
//
//        if (hotels.isEmpty()) {
//            logger.warn("No hotels found for the matching rooms and district: {}", district);
//            return List.of();
//        }
//
//        // Tạo danh sách phản hồi
//        List<HotelSearchResponse> responses = new ArrayList<>();
//        for (Hotel hotel : hotels) {
//            HotelSearchResponse response = new HotelSearchResponse();
//            response.setHotel(toHotelDTO(hotel));
//
//            List<RoomType> hotelRooms = roomsByHotel.get(hotel.getId());
//            List<RoomDTO> roomDTOs = hotelRooms.stream()
//                    .map(this::toRoomDTO)
//                    .collect(Collectors.toList());
//            response.setRooms(roomDTOs);
//            response.setPlaces(List.of()); // Tạm thời để trống, có thể cập nhật sau
//
//            responses.add(response);
//        }
//
//        logger.info("Found {} hotels with matching rooms in district: {}", responses.size(), district);
//        return responses;
//    }
    @Transactional(readOnly = true)
    public List<HotelSearchResponse> searchHotelsByPriceGuestsAndDistrict(Integer maxPrice, Integer numberOfGuests, String district) {
        logger.info("Searching hotels with maxPrice: {}, numberOfGuests: {}, district: {}", maxPrice, numberOfGuests, district);

        // Kiểm tra xem có ít nhất một tiêu chí được cung cấp không
        if (maxPrice == null && numberOfGuests == null && district == null) {
            throw new IllegalArgumentException("At least one search criterion (maxPrice, numberOfGuests, or district) must be provided");
        }

        // Kiểm tra giá trị hợp lệ nếu các trường được cung cấp
        if (maxPrice != null && maxPrice <= 0) {
            throw new IllegalArgumentException("maxPrice must be a positive value");
        }
        if (numberOfGuests != null && numberOfGuests <= 0) {
            throw new IllegalArgumentException("numberOfGuests must be a positive value");
        }
        List<String> validDistricts = Arrays.asList("Liên Chiểu", "Hải Châu", "Thanh Khê", "Sơn Trà", "Ngũ Hành Sơn", "Cẩm Lệ");
        if (district != null && !district.isEmpty() && !validDistricts.contains(district)) {
            throw new IllegalArgumentException("District must be one of: " + validDistricts);
        }

        // Tìm các phòng thỏa mãn maxPrice và numberOfGuests (nếu có)
        List<RoomType> matchingRooms;
        if (maxPrice != null && numberOfGuests != null) {
            matchingRooms = roomRepository.findByPriceAndGuests(Double.valueOf(maxPrice), numberOfGuests);
        } else if (maxPrice != null) {
            matchingRooms = roomRepository.findByPrice(Double.valueOf(maxPrice));
        } else if (numberOfGuests != null) {
            matchingRooms = roomRepository.findByGuests(numberOfGuests);
        } else {
            // Nếu không có maxPrice và numberOfGuests, lấy tất cả phòng
            matchingRooms = roomRepository.findAll();
        }

        if (matchingRooms.isEmpty()) {
            logger.info("No rooms found matching the criteria: maxPrice = {}, numberOfGuests = {}", maxPrice, numberOfGuests);
            return List.of();
        }

        // Gom nhóm phòng theo hotelId
        Map<Integer, List<RoomType>> roomsByHotel = new HashMap<>();
        for (RoomType room : matchingRooms) {
            Integer hotelId = room.getHotel().getId();
            roomsByHotel.computeIfAbsent(hotelId, k -> new ArrayList<>()).add(room);
        }

        // Lấy danh sách hotelId
        List<Integer> hotelIds = new ArrayList<>(roomsByHotel.keySet());

        // Tìm khách sạn theo hotelIds và district (nếu có)
        List<Hotel> hotels;
        if (district != null && !district.isEmpty()) {
            hotels = hotelRepository.findAllByIdAndDistrict(hotelIds, district);
        } else {
            hotels = hotelRepository.findAllById(hotelIds);
        }

        if (hotels.isEmpty()) {
            logger.warn("No hotels found for the matching rooms and district: {}", district);
            return List.of();
        }

        // Tạo danh sách phản hồi
        List<HotelSearchResponse> responses = new ArrayList<>();
        for (Hotel hotel : hotels) {
            HotelSearchResponse response = new HotelSearchResponse();
            response.setHotel(toHotelDTO(hotel));

            List<RoomType> hotelRooms = roomsByHotel.get(hotel.getId());
            List<RoomDTO> roomDTOs = hotelRooms.stream()
                    .map(this::toRoomDTO)
                    .collect(Collectors.toList());
            response.setRooms(roomDTOs);
            response.setPlaces(List.of()); // Tạm thời để trống, có thể cập nhật sau

            responses.add(response);
        }

        logger.info("Found {} hotels with matching rooms in district: {}", responses.size(), district);
        return responses;
    }

    // ko dùng nữa
    @Transactional(readOnly = true)
    public List<HotelSearchResponse> searchHotelsByPriceAndGuests(Integer maxPrice, Integer numberOfGuests) {
        logger.info("Searching hotels with maxPrice: {} and numberOfGuests: {}", maxPrice, numberOfGuests);

        if (maxPrice == null || maxPrice <= 0) {
            throw new IllegalArgumentException("maxPrice must be a positive value");
        }
        if (numberOfGuests == null || numberOfGuests <= 0) {
            throw new IllegalArgumentException("numberOfGuests must be a positive value");
        }

        List<RoomType> matchingRooms = roomRepository.findByPriceAndGuests(Double.valueOf(maxPrice), numberOfGuests);
        if (matchingRooms.isEmpty()) {
            logger.info("No rooms found matching the criteria: maxPrice = {}, numberOfGuests = {}", maxPrice, numberOfGuests);
            return List.of();
        }

        Map<Integer, List<RoomType>> roomsByHotel = new HashMap<>();
        for (RoomType room : matchingRooms) {
            Integer hotelId = room.getHotel().getId();
            roomsByHotel.computeIfAbsent(hotelId, k -> new ArrayList<>()).add(room);
        }

        List<Integer> hotelIds = new ArrayList<>(roomsByHotel.keySet());
        List<Hotel> hotels = hotelRepository.findAllById(hotelIds);
        if (hotels.isEmpty()) {
            logger.warn("No hotels found for the matching rooms");
            return List.of();
        }

        List<HotelSearchResponse> responses = new ArrayList<>();
        for (Hotel hotel : hotels) {
            HotelSearchResponse response = new HotelSearchResponse();
            response.setHotel(toHotelDTO(hotel));

            List<RoomType> hotelRooms = roomsByHotel.get(hotel.getId());
            List<RoomDTO> roomDTOs = hotelRooms.stream()
                    .map(this::toRoomDTO)
                    .collect(Collectors.toList());
            response.setRooms(roomDTOs);
            response.setPlaces(List.of()); // Tạm thời để trống, có thể cập nhật sau

            responses.add(response);
        }

        logger.info("Found {} hotels with matching rooms", responses.size());
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

    // Phương thức chuyển đổi từ Hotel sang HotelDTO
    private HotelDTO toHotelDTO(Hotel hotel) {
        HotelDTO hotelDTO = new HotelDTO();
        hotelDTO.setId(hotel.getId());
        hotelDTO.setName(hotel.getName());
//        hotelDTO.setAddress(hotel.getAddress());
        hotelDTO.setDistrict(hotel.getDistrict());
        hotelDTO.setDescription(hotel.getDescription());
        hotelDTO.setHotelLink(hotel.getHotelLink());
        hotelDTO.setRatingStars(hotel.getRatingStars());
        hotelDTO.setFacilities(hotel.getFacilities());
        hotelDTO.setHighlights(hotel.getHighlights());
        hotelDTO.setReviews(hotel.getReviews());
        hotelDTO.setImageUrls(hotel.getImageUrls());
        hotelDTO.setRoomServices(hotel.getRoomServices());
        hotelDTO.setSlug(hotel.getSlug());
        hotelDTO.setLatitude(hotel.getCoordinates() != null ? hotel.getCoordinates().getY() : null);
        hotelDTO.setLongitude(hotel.getCoordinates() != null ? hotel.getCoordinates().getX() : null);
        return hotelDTO;
    }

    // Phương thức chuyển đổi từ RoomType sang RoomDTO
    private RoomDTO toRoomDTO(RoomType roomType) {
        RoomDTO roomDTO = new RoomDTO();
        roomDTO.setId(roomType.getId());
        roomDTO.setHotelId(roomType.getHotel().getId());
        roomDTO.setName(roomType.getName());
        roomDTO.setNumberOfGuests(roomType.getNumberOfGuests());
        roomDTO.setPrice(roomType.getPrice());
        roomDTO.setOriginalPrice(roomType.getOriginalPrice());
        roomDTO.setTaxesAndFeesUnderPrice(roomType.getTaxesAndFeesUnderPrice());
        return roomDTO;
    }

    // Phương thức chuyển đổi từ Place sang PlaceDTO
    private PlaceDTO toPlaceDTO(Place place) {
        PlaceDTO placeDTO = new PlaceDTO();
        placeDTO.setId(place.getId());
        placeDTO.setTitle(place.getTitle());
        placeDTO.setRating(place.getRating());
        placeDTO.setAddress(place.getAddress());
        placeDTO.setReview(place.getReview());
        placeDTO.setSlug(place.getSlug());
        placeDTO.setLatitude(place.getCoordinates() != null ? place.getCoordinates().getY() : null);
        placeDTO.setLongitude(place.getCoordinates() != null ? place.getCoordinates().getX() : null);
        placeDTO.setImageUrl(place.getImageUrl());
        placeDTO.setDescription(place.getDescription());
        placeDTO.setServices(place.getServices());
        return placeDTO;
    }
}