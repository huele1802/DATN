package com.example.AI.Hotel.service;

import com.example.AI.Hotel.dto.HotelDTO;
import com.example.AI.Hotel.dto.HotelSearchResponse;
import com.example.AI.Hotel.dto.PlaceDTO;
import com.example.AI.Hotel.dto.RoomDTO;
import com.example.AI.Hotel.model.Hotel;
import com.example.AI.Hotel.model.Place;
import com.example.AI.Hotel.model.RoomType;
import com.example.AI.Hotel.repository.HotelRepository;
import com.example.AI.Hotel.repository.PlaceRepository;
import com.example.AI.Hotel.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HotelDataService {
    private static final Logger logger = LoggerFactory.getLogger(HotelDataService.class);

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final PlaceRepository placeRepository;

    @Autowired
    public HotelDataService(
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            PlaceRepository placeRepository
    ) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.placeRepository = placeRepository;
    }

    @Transactional(readOnly = true)
    public Page<HotelSearchResponse> getAllHotels(int page, int size) {
        logger.info("Fetching hotels with pagination - page: {}, size: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<Hotel> hotelsPage = hotelRepository.findAll(pageable);
            logger.info("Found {} hotels in page {}", hotelsPage.getTotalElements(), page);

            if (hotelsPage.isEmpty()) {
                logger.warn("No hotels found for page: {}", page);
                return Page.empty(pageable);
            }

            List<HotelSearchResponse> responses = hotelsPage.getContent().stream()
                    .map(this::mapToHotelSearchResponse)
                    .collect(Collectors.toList());
            return new PageImpl<>(responses, pageable, hotelsPage.getTotalElements());

        } catch (Exception e) {
            logger.error("Error fetching hotels for page: {}, size: {}", page, size, e);
            throw new RuntimeException("Error fetching hotels: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<RoomDTO> getAllRooms(int page, int size) {
        logger.info("Fetching rooms with pagination - page: {}, size: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<RoomType> roomsPage = roomRepository.findAll(pageable);
            logger.info("Found {} rooms in page {}", roomsPage.getTotalElements(), page);

            if (roomsPage.isEmpty()) {
                logger.warn("No rooms found for page: {}", page);
                return Page.empty(pageable);
            }

            List<RoomDTO> roomDTOs = roomsPage.getContent().stream()
                    .map(this::mapToRoomDTO)
                    .collect(Collectors.toList());
            return new PageImpl<>(roomDTOs, pageable, roomsPage.getTotalElements());

        } catch (Exception e) {
            logger.error("Error fetching rooms for page: {}, size: {}", page, size, e);
            throw new RuntimeException("Error fetching rooms: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<PlaceDTO> getAllPlaces(int page, int size) {
        logger.info("Fetching places with pagination - page: {}, size: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<Place> placesPage = placeRepository.findAll(pageable);
            logger.info("Found {} places in page {}", placesPage.getTotalElements(), page);

            if (placesPage.isEmpty()) {
                logger.warn("No places found for page: {}", page);
                return Page.empty(pageable);
            }

            List<PlaceDTO> placeDTOs = placesPage.getContent().stream()
                    .map(this::mapToPlaceDTO)
                    .collect(Collectors.toList());
            return new PageImpl<>(placeDTOs, pageable, placesPage.getTotalElements());

        } catch (Exception e) {
            logger.error("Error fetching places for page: {}, size: {}", page, size, e);
            throw new RuntimeException("Error fetching places: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public HotelSearchResponse getHotelById(Integer id) {
        logger.info("Fetching hotel with id: {}", id);
        try {
            Optional<Hotel> hotelOpt = hotelRepository.findById(id);
            if (hotelOpt.isEmpty()) {
                logger.warn("Hotel not found for id: {}", id);
                throw new RuntimeException("Hotel not found with id: " + id);
            }

            Hotel hotel = hotelOpt.get();
            return mapToHotelSearchResponse(hotel);

        } catch (Exception e) {
            logger.error("Error fetching hotel with id: {}", id, e);
            throw new RuntimeException("Error fetching hotel: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public HotelSearchResponse findHotelBySlug(String slug) {
        logger.info("Fetching hotel with slug: {}", slug);
        try {
            Optional<Hotel> hotelOptional = hotelRepository.findBySlug(slug);
            if (hotelOptional.isEmpty()) {
                logger.warn("Hotel not found for slug: {}", slug);
                throw new RuntimeException("Hotel not found with id: " + slug);
            }

            Hotel hotel = hotelOptional.get();
            return mapToHotelSearchResponse(hotel);

        } catch (Exception e) {
            logger.error("Error fetching hotel with slug: {}", slug, e);
            throw new RuntimeException("Error fetching hotel: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public RoomDTO getRoomById(Integer id) {
        logger.info("Fetching room with id: {}", id);
        try {
            Optional<RoomType> roomOpt = roomRepository.findById(id);
            if (roomOpt.isEmpty()) {
                logger.warn("Room not found for id: {}", id);
                throw new RuntimeException("Room not found with id: " + id);
            }

            RoomType room = roomOpt.get();
            return mapToRoomDTO(room);

        } catch (Exception e) {
            logger.error("Error fetching room with id: {}", id, e);
            throw new RuntimeException("Error fetching room: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PlaceDTO getPlaceById(Integer id) {
        logger.info("Fetching place with id: {}", id);
        try {
            Optional<Place> placeOpt = placeRepository.findById(id);
            if (placeOpt.isEmpty()) {
                logger.warn("Place not found for id: {}", id);
                throw new RuntimeException("Place not found with id: " + id);
            }

            Place place = placeOpt.get();
            return mapToPlaceDTO(place);

        } catch (Exception e) {
            logger.error("Error fetching place with id: {}", id, e);
            throw new RuntimeException("Error fetching place: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PlaceDTO getPlaceBySlug(String slug) {
        logger.info("Fetching place with slug: {}", slug);
        try {
            Optional<Place> placeOpt = placeRepository.findBySlug(slug);
            if (placeOpt.isEmpty()) {
                logger.warn("Place not found for slug: {}", slug);
                throw new RuntimeException("Place not found with id: " + slug);
            }

            Place place = placeOpt.get();
            return mapToPlaceDTO(place);

        } catch (Exception e) {
            logger.error("Error fetching place with slug: {}", slug, e);
            throw new RuntimeException("Error fetching place: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<HotelSearchResponse> getTop5HotelsByReviews() {
        logger.info("Fetching top 5 hotels by reviews");
        try {
            // Lấy tất cả khách sạn từ repository
            List<Hotel> allHotels = hotelRepository.findAll();
            if (allHotels.isEmpty()) {
                logger.warn("No hotels found in the database");
                return Collections.emptyList();
            }

            // Sắp xếp khách sạn theo điểm review trung bình giảm dần và lấy top 5
            List<HotelSearchResponse> topHotels = allHotels.stream()
                    .filter(hotel -> hotel.getReviews() != null && !hotel.getReviews().isEmpty()) // Bỏ qua khách sạn không có review
                    .sorted(Comparator.comparingDouble(hotel ->
                            -hotel.getReviews().values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0))) // Sắp xếp giảm dần theo điểm trung bình
                    .limit(5) // Lấy top 5
                    .map(this::mapToHotelSearchResponse) // Ánh xạ sang HotelSearchResponse
                    .collect(Collectors.toList());

            logger.info("Successfully fetched top 5 hotels by reviews, count: {}", topHotels.size());
            return topHotels;

        } catch (Exception e) {
            logger.error("Error fetching top 5 hotels by reviews", e);
            throw new RuntimeException("Error fetching top 5 hotels by reviews: " + e.getMessage(), e);
        }
    }
    private HotelSearchResponse mapToHotelSearchResponse(Hotel hotel) {
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

        HotelSearchResponse response = new HotelSearchResponse();
        response.setHotel(hotelDTO);
        response.setSimilarityScore(null); // Không có similarity score trong trường hợp này
//        response.setRooms(Collections.emptyList()); // Để trống nếu không lấy rooms
//        response.setPlaces(Collections.emptyList()); // Để trống nếu không lấy places
        return response;
    }

    private RoomDTO mapToRoomDTO(RoomType roomType) {
        RoomDTO dto = new RoomDTO();
        dto.setId(roomType.getId());
        dto.setHotelId(roomType.getHotel() != null ? roomType.getHotel().getId() : null);
        dto.setName(roomType.getName());
        dto.setNumberOfGuests(roomType.getNumberOfGuests());
        dto.setPrice(roomType.getPrice());
        dto.setOriginalPrice(roomType.getOriginalPrice());
        dto.setTaxesAndFeesUnderPrice(roomType.getTaxesAndFeesUnderPrice());
        return dto;
    }

    private PlaceDTO mapToPlaceDTO(Place place) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(place.getId());
        dto.setTitle(place.getTitle());
        dto.setRating(place.getRating());
        dto.setAddress(place.getAddress());
        dto.setReview(place.getReview());
        dto.setSlug(place.getSlug());
        dto.setLatitude(place.getCoordinates() != null ? place.getCoordinates().getY() : null);
        dto.setLongitude(place.getCoordinates() != null ? place.getCoordinates().getX() : null);
        dto.setImageUrl(place.getImageUrl());
        dto.setDescription(place.getDescription());
        dto.setServices(place.getServices());
        return dto;
    }
}