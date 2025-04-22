package com.example.AI.Hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {
    private Integer placeId;
    private String title;
    private String rating;
    private String reviewCount;
    private String address;
    private CoordinatesDTO coordinates;
    private String phoneNumber;
}
