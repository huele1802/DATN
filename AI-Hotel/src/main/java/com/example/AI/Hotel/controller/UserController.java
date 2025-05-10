package com.example.AI.Hotel.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.AI.Hotel.dto.SearchHistoryDTO;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
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

    @Autowired
    private Cloudinary cloudinary;

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

//    @PutMapping(value = "/profile", consumes = "multipart/form-data")
//    public ResponseEntity<Map<String, Object>> updateProfile(
//            @Valid @RequestBody UpdateProfileRequest request,
//            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            String email = SecurityContextHolder.getContext().getAuthentication().getName();
//            User user = userRepository.findByEmail(email)
//                    .orElseThrow(() -> new IllegalStateException("User not found"));
//
//            // Cập nhật thông tin hồ sơ
//            if (request.getFullName() != null) {
//                user.setFullName(request.getFullName());
//            }
//            if (request.getPhoneNumber() != null) {
//                user.setPhoneNumber(request.getPhoneNumber());
//            }
//            if (request.getDateOfBirth() != null) {
//                user.setDateOfBirth(request.getDateOfBirth());
//            }
//            if (request.getAddress() != null) {
//                user.setAddress(request.getAddress());
//            }
//
//            // Xử lý file avatar nếu có
//            if (avatar != null && !avatar.isEmpty()) {
//                // Kiểm tra định dạng file
//                String contentType = avatar.getContentType();
//                if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg"))) {
//                    response.put("message", "Only image files (jpg, png, jpeg) are allowed");
//                    response.put("status", 400);
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//                }
//
//                // Kiểm tra kích thước file
//                if (avatar.getSize() > 10 * 1024 * 1024) { // Giới hạn 10MB
//                    response.put("message", "File size must be less than 10MB");
//                    response.put("status", 400);
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//                }
//
//                // Upload lên Cloudinary
//                Map uploadResult = cloudinary.uploader().upload(avatar.getBytes(), ObjectUtils.asMap(
//                        "resource_type", "image",
//                        "public_id", email + "_" + System.currentTimeMillis()
//                ));
//                String newAvatarUrl = (String) uploadResult.get("secure_url");
//
//                // Xóa avatar cũ trên Cloudinary nếu có (trừ avatar mặc định)
//                if (user.getAvatarUrl() != null && !user.getAvatarUrl().contains("default-avatar")) {
//                    String oldPublicId = extractPublicId(user.getAvatarUrl());
//                    cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
//                }
//
//                // Cập nhật URL mới
//                user.setAvatarUrl(newAvatarUrl);
//            }
//
//            userRepository.save(user);
//
//            response.put("message", "User updated successfully");
//            response.put("status", 200);
//            response.put("avatarUrl", user.getAvatarUrl());
//            return ResponseEntity.ok(response);
//        } catch (IllegalStateException e) {
//            response.put("message", "User not found");
//            response.put("status", 404);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
//        } catch (IOException e) {
//            response.put("message", "Failed to upload avatar: " + e.getMessage());
//            response.put("status", 500);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        } catch (Exception e) {
//            response.put("message", "Failed to update user: " + e.getMessage());
//            response.put("status", 500);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//


    @PutMapping(value = "/profile", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        Map<String, Object> response = new HashMap<>();

        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            // Cập nhật thông tin hồ sơ
            if (fullName != null && !fullName.isEmpty()) {
                if (fullName.length() < 2 || fullName.length() > 100) {
                    response.put("message", "INVALID_FULLNAME_FORMAT: Full name must be between 2 and 100 characters");
                    response.put("status", 400);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
                user.setFullName(fullName);
            }
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                if (phoneNumber.length() < 10 || phoneNumber.length() > 15) {
                    response.put("message", "INVALID_PHONE_FORMAT: Phone number must be between 10 and 15 characters");
                    response.put("status", 400);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
                user.setPhoneNumber(phoneNumber);
            }
            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    LocalDate parsedDate = LocalDate.parse(dateOfBirth, formatter);
                    user.setDateOfBirth(parsedDate);
                } catch (DateTimeParseException e) {
                    response.put("message", "INVALID_DATE_FORMAT: Date of birth must be in format yyyy-MM-dd");
                    response.put("status", 400);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }
            if (address != null && !address.isEmpty()) {
                if (address.length() > 255) {
                    response.put("message", "INVALID_ADDRESS_FORMAT: Address must not exceed 255 characters");
                    response.put("status", 400);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
                user.setAddress(address);
            }

            // Xử lý file avatar nếu có
            if (avatar != null && !avatar.isEmpty()) {
                // Kiểm tra định dạng file
                String contentType = avatar.getContentType();
                if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg"))) {
                    response.put("message", "Only image files (jpg, png, jpeg) are allowed");
                    response.put("status", 400);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                // Kiểm tra kích thước file
                if (avatar.getSize() > 10 * 1024 * 1024) { // Giới hạn 10MB
                    response.put("message", "File size must be less than 10MB");
                    response.put("status", 400);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                // Upload lên Cloudinary
                Map uploadResult = cloudinary.uploader().upload(avatar.getBytes(), ObjectUtils.asMap(
                        "resource_type", "image",
                        "public_id", email + "_" + System.currentTimeMillis()
                ));
                String newAvatarUrl = (String) uploadResult.get("secure_url");

                // Xóa avatar cũ trên Cloudinary nếu có (trừ avatar mặc định)
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().contains("default-avatar")) {
                    String oldPublicId = extractPublicId(user.getAvatarUrl());
                    cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
                }

                // Cập nhật URL mới
                user.setAvatarUrl(newAvatarUrl);
            }

            userRepository.save(user);

            response.put("message", "User updated successfully");
            response.put("status", 200);
            response.put("avatarUrl", user.getAvatarUrl());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("message", "User not found");
            response.put("status", 404);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IOException e) {
            response.put("message", "Failed to upload avatar: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("message", "Failed to update user: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String extractPublicId(String url) {
        String[] parts = url.split("/");
        String fileName = parts[parts.length - 1];
        return fileName.substring(0, fileName.lastIndexOf("."));
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

