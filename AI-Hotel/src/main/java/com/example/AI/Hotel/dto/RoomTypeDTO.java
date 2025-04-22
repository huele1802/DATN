package com.example.AI.Hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeDTO {
    private Integer roomId;
    private String name;
    private Integer numberOfGuests;
    private Integer price;
    private double roomSimilarityScore;
}
