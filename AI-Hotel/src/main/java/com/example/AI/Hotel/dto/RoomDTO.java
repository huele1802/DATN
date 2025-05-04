package com.example.AI.Hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private Integer id;
    private Integer hotelId;
    private String name;
    private Integer numberOfGuests;
    private Integer price;
    private Integer originalPrice;
    private Boolean taxesAndFeesUnderPrice;
}
