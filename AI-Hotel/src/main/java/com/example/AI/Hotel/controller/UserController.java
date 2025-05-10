package com.example.AI.Hotel.controller;

import com.example.AI.Hotel.dto.SearchHistoryDTO;
import com.example.AI.Hotel.dto.UpdateProfileRequest;
import com.example.AI.Hotel.model.SearchHistory;
import com.example.AI.Hotel.model.User;
import com.example.AI.Hotel.repository.SearchHistoryRepository;
import com.example.AI.Hotel.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/search-history")
    public ResponseEntity<List<SearchHistoryDTO>> getSearchHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        List<SearchHistory> history = searchHistoryRepository.findByUserId(user.getId());
        List<SearchHistoryDTO> historyDTOs = history.stream()
                .map(searchHistory -> new SearchHistoryDTO(
                        searchHistory.getId(),
                        searchHistory.getQueryHistory()
//                        searchHistory.getSearchDate()
                ))
                .toList();

        return ResponseEntity.ok(historyDTOs);
    }

    @PutMapping(value = "/profile", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> updateProfile(@Valid @RequestPart("request") UpdateProfileRequest request
                                                             ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            if (request.getFullName() != null) {
                user.setFullName(request.getFullName());
            }
            if (request.getPhoneNumber() != null) {
                user.setPhoneNumber(request.getPhoneNumber());
            }
            if (request.getDateOfBirth() != null) {
                user.setDateOfBirth(request.getDateOfBirth());
            }
            if (request.getAddress() != null) {
                user.setAddress(request.getAddress());
            }
            if (request.getAvatarUrl() != null) {
                user.setAvatarUrl(request.getAvatarUrl());
            }

            userRepository.save(user);

            response.put("message", "User update successfully");
            response.put("status", 200);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("message", "User not found");
            response.put("status", 404);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("message", "Failed to update user: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    // kiểm tra bằng cách gọi token
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return ResponseEntity.ok(user);
    }
}

