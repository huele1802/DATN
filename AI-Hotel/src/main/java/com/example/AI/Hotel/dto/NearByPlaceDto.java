package com.example.AI.Hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearByPlaceDto {

    private Integer placeId;
    private String title;
    private Double distanceInMeters;
}
