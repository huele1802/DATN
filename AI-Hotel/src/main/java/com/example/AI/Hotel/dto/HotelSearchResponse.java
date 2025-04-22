package com.example.AI.Hotel.dto;

import com.example.AI.Hotel.model.Hotel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelSearchResponse {
    private Integer hotelId;
    private String name;
    private String description;
    private List<String> facilities;
    private Map<String, String> reviews;
    private Integer ratingStars;
    private String address;
    private CoordinatesDTO coordinates;
    private List<RoomTypeDTO> rooms;
    private List<PlaceDTO> places;
    private double similarityScore;
}
