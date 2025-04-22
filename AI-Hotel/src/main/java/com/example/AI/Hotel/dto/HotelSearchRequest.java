package com.example.AI.Hotel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HotelSearchRequest {
    @NotBlank(message = "Query cannot be empty")
    private String query;

//    private Integer limit = 10; // Default number of results
}