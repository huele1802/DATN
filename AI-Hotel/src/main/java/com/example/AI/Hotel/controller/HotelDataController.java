package com.example.AI.Hotel.controller;

import com.example.AI.Hotel.dto.HotelDTO;
import com.example.AI.Hotel.dto.HotelSearchResponse;
import com.example.AI.Hotel.dto.PlaceDTO;
import com.example.AI.Hotel.dto.RoomDTO;
import com.example.AI.Hotel.service.HotelDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/getAll")
public class HotelDataController {

    private final HotelDataService hotelDataService;

    @Autowired
    public HotelDataController(HotelDataService hotelDataService) {
        this.hotelDataService = hotelDataService;
    }

    @GetMapping("/hotels")
    public ResponseEntity<Map<String, Object>> getAllHotels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // Không cần trừ 1 từ page, vì Spring Data đã xử lý one-based index
            Page<HotelSearchResponse> hotelsPage = hotelDataService.getAllHotels(page, size);
            return buildPagedResponse(hotelsPage, "Không tìm thấy khách sạn");
        } catch (Exception e) {
            log.error("Error fetching all hotels: page={}, size={}", page, size, e);
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/hotels/{id}")
    public ResponseEntity<Map<String, Object>> getHotelById(@PathVariable Integer id) {
        try {
            HotelSearchResponse hotel = hotelDataService.getHotelById(id);
            return buildDetailResponse(hotel, "hotel", "Không tìm thấy khách sạn với id: " + id);
        } catch (Exception e) {
            log.error("Error fetching hotel with id: {}", id, e);
            return buildErrorResponse(e);
        }
    }
    @GetMapping("/hotel/{slug}")
    public ResponseEntity<Map<String, Object>> getHotelBySlug(@PathVariable String slug) {
        try {
            HotelSearchResponse hotel = hotelDataService.findHotelBySlug(slug);
            return buildDetailResponse(hotel, "hotel", "Không tìm thấy khách sạn với slug: " + slug);
        } catch (Exception e) {
            log.error("Error fetching hotel with slug: {}", slug, e);
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/top-5-hotels-by-reviews")
    public ResponseEntity<Map<String, Object>> getTop5HotelsByReviews() {
        try {
            List<HotelSearchResponse> hotels = hotelDataService.getTop5HotelsByReviews();
            if (hotels.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No hotels found with reviews");
                response.put("status", HttpStatus.NOT_FOUND.value());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Top 5 hotels by reviews retrieved successfully");
            response.put("status", HttpStatus.OK.value());
            response.put("data", hotels);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching top 5 hotels by reviews", e);
            return buildErrorResponse(e);
        }
    }
    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getAllRooms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // Không cần trừ 1 từ page, vì Spring Data đã xử lý one-based index
            Page<RoomDTO> roomsPage = hotelDataService.getAllRooms(page, size);
            return buildPagedResponse(roomsPage, "Không tìm thấy phòng");
        } catch (Exception e) {
            log.error("Error fetching all rooms: page={}, size={}", page, size, e);
            return buildErrorResponse(e);
        }
    }


    @GetMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> getRoomById(@PathVariable Integer id) {
        try {
            RoomDTO room = hotelDataService.getRoomById(id);
            return buildDetailResponse(room, "room", "Không tìm thấy phòng với id: " + id);
        } catch (Exception e) {
            log.error("Error fetching room with id: {}", id, e);
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/places")
    public ResponseEntity<Map<String, Object>> getAllPlaces(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // Không cần trừ 1 từ page, vì Spring Data đã xử lý one-based index
            Page<PlaceDTO> placesPage = hotelDataService.getAllPlaces(page, size);
            return buildPagedResponse(placesPage, "Không tìm thấy địa điểm");
        } catch (Exception e) {
            log.error("Error fetching all places: page={}, size={}", page, size, e);
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/places/{id}")
    public ResponseEntity<Map<String, Object>> getPlaceById(@PathVariable Integer id) {
        try {
            PlaceDTO place = hotelDataService.getPlaceById(id);
            return buildDetailResponse(place, "place", "Không tìm thấy địa điểm với id: " + id);
        } catch (Exception e) {
            log.error("Error fetching place with id: {}", id, e);
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/place/{slug}")
    public ResponseEntity<Map<String, Object>> getPlaceBySlug(@PathVariable String slug) {
        try {
            PlaceDTO place = hotelDataService.getPlaceBySlug(slug);
            return buildDetailResponse(place, "place", "Không tìm thấy địa điểm với slug: " + slug);
        } catch (Exception e) {
            log.error("Error fetching place with id: {}", slug, e);
            return buildErrorResponse(e);
        }
    }

    // Method dùng chung để build response phân trang
    private <T> ResponseEntity<Map<String, Object>> buildPagedResponse(Page<T> pageData, String notFoundMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", pageData.getContent());
        response.put("currentPage", pageData.getNumber() + 1); // Chuyển về one-based index (bắt đầu từ 1)
        response.put("totalItems", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());

        if (pageData.getContent().isEmpty()) {
            response.put("message", notFoundMessage);
            return ResponseEntity.status(404).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // Method dùng chung để build response chi tiết
    private <T> ResponseEntity<Map<String, Object>> buildDetailResponse(T data, String key, String notFoundMessage) {
        Map<String, Object> response = new HashMap<>();
        if (data == null) {
            response.put("message", notFoundMessage);
            return ResponseEntity.status(404).body(response);
        }
        response.put(key, data);
        return ResponseEntity.ok(response);
    }

    // Method dùng chung để build response lỗi
    private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Lỗi máy chủ: " + e.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }
}