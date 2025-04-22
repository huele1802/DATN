package com.example.AI.Hotel.service;

import com.example.AI.Hotel.dto.HotelSearchResponse;
import com.example.AI.Hotel.dto.PlaceDTO;
import com.example.AI.Hotel.dto.RoomTypeDTO;
import com.example.AI.Hotel.model.Hotel;
import com.example.AI.Hotel.model.Place;
import com.example.AI.Hotel.model.RoomType;
import com.example.AI.Hotel.repository.HotelRepository;
import com.example.AI.Hotel.repository.PlaceRepository;
import com.example.AI.Hotel.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<HotelSearchResponse> getAllHotels() {
        logger.info("Fetching all hotels...");
        List<Hotel> hotels = hotelRepository.findAll();
        logger.info("Found {} hotels", hotels.size());

        return hotels.stream().map(this::mapToHotel).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomTypeDTO> getAllRooms() {
        logger.info("Fetching all rooms...");
        List<RoomType> rooms = roomRepository.findAll();
        logger.info("Found {} rooms", rooms.size());

        return rooms.stream().map(this::mapToRoomTypeDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlaceDTO> getAllPlaces() {
        logger.info("Fetching all places...");
        List<Place> places = placeRepository.findAll();
        logger.info("Found {} places", places.size());

        return places.stream().map(this::mapToPlaceDTO).collect(Collectors.toList());
    }

    private HotelSearchResponse mapToHotel(Hotel hotel) {
        HotelSearchResponse dto = new HotelSearchResponse();
        dto.setHotelId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setDescription(hotel.getDescription());
        dto.setFacilities(hotel.getFacilities());
        dto.setReviews(hotel.getReviews());
        dto.setRatingStars(hotel.getRatingStars());
        dto.setAddress(hotel.getAddress());
        return dto;
    }

    private RoomTypeDTO mapToRoomTypeDTO(RoomType roomType) {
        RoomTypeDTO dto = new RoomTypeDTO();
        dto.setRoomId(roomType.getId());
        dto.setName(roomType.getName());
        dto.setNumberOfGuests(roomType.getNumberOfGuests());
        dto.setPrice(roomType.getPrice());
        return dto;
    }

    private PlaceDTO mapToPlaceDTO(Place place) {
        PlaceDTO dto = new PlaceDTO();
        dto.setPlaceId(place.getId());
        dto.setTitle(place.getTitle());
        dto.setRating(place.getRating());
        dto.setReviewCount(place.getReviewCount());
        dto.setAddress(place.getAddress());
        dto.setPhoneNumber(place.getPhoneNumber());
        return dto;
    }
}