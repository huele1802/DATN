package com.example.AI.Hotel.controller;

import com.example.AI.Hotel.dto.HotelSearchResponse;
import com.example.AI.Hotel.dto.PlaceDTO;
import com.example.AI.Hotel.dto.RoomTypeDTO;
import com.example.AI.Hotel.service.HotelDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/getAll")
public class HotelDataController {

    private final HotelDataService hotelDataService;

    @Autowired
    public HotelDataController(HotelDataService hotelDataService) {
        this.hotelDataService = hotelDataService;
    }

    @GetMapping("/hotels")
    public ResponseEntity<List<HotelSearchResponse>> getAllHotels() {
        List<HotelSearchResponse> hotels = hotelDataService.getAllHotels();
        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomTypeDTO>> getAllRooms() {
        List<RoomTypeDTO> rooms = hotelDataService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/places")
    public ResponseEntity<List<PlaceDTO>> getAllPlaces() {
        List<PlaceDTO> places = hotelDataService.getAllPlaces();
        return ResponseEntity.ok(places);
    }
}