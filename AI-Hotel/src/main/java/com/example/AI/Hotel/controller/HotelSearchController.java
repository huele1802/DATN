package com.example.AI.Hotel.controller;

import com.example.AI.Hotel.dto.HotelSearchRequest;
import com.example.AI.Hotel.dto.HotelSearchResponse;
import com.example.AI.Hotel.dto.NearByPlaceDto;
import com.example.AI.Hotel.service.HotelSearchService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/hotels")
public class HotelSearchController {

    private final HotelSearchService searchService;
    private final HotelSearchService hotelSearchService;

    @Autowired
    public HotelSearchController(HotelSearchService searchService, HotelSearchService hotelSearchService) {
        this.searchService = searchService;
        this.hotelSearchService = hotelSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchHotels(
            @Valid @RequestBody HotelSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {

            List<HotelSearchResponse> responses = searchService.searchHotels(request);
            log.info("Search completed for query: {}, found {} hotels", request.getQuery(), responses.size());

            // Phân trang kết quả
            Pageable pageable = PageRequest.of(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), responses.size());
            List<HotelSearchResponse> pagedResponses = start < responses.size()
                    ? responses.subList(start, end)
                    : List.of();

            Page<HotelSearchResponse> pagedResult = new PageImpl<>(pagedResponses, pageable, responses.size());

            // Tạo phản hồi
            Map<String, Object> response = new HashMap<>();
            response.put("hotels", pagedResult.getContent());
            response.put("currentPage", pagedResult.getNumber());
            response.put("totalItems", pagedResult.getTotalElements());
            response.put("totalPages", pagedResult.getTotalPages());

            if (pagedResponses.isEmpty() && responses.isEmpty()) {
                log.warn("No hotels found for query: {}", request.getQuery());
                response.put("message", "Không tìm thấy khách sạn phù hợp");
                return ResponseEntity.status(404).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing search query: {}", request.getQuery(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Lỗi máy chủ: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/{hotelId}/nearby-places")
    public ResponseEntity<List<NearByPlaceDto>> getNearbyPlaces(
            @PathVariable Integer hotelId,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Integer limit) {
        List<NearByPlaceDto> nearbyPlaces = hotelSearchService.findNearbyPlaces(hotelId, maxDistance, limit);
        return ResponseEntity.ok(nearbyPlaces);
    }

    @GetMapping("/search-by-price-and-guests")
    public ResponseEntity<List<HotelSearchResponse>> searchHotelsByPriceAndGuests(
            @RequestParam Double maxPrice,
            @RequestParam Integer numberOfGuests) {
        List<HotelSearchResponse> results = hotelSearchService.searchHotelsByPriceAndGuests(maxPrice, numberOfGuests);
        return ResponseEntity.ok(results);
    }
    // Xử lý lỗi validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Validation failed for request: {}", errors);
        return ResponseEntity.status(400).body(errors);
    }
}