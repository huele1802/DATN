package com.example.AI.Hotel.controller;

import com.example.AI.Hotel.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PutMapping("/users/{userId}/disable")
    public ResponseEntity<Map<String, Object>> disableUser(@PathVariable Integer userId) {
        try {
            adminService.disableUser(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User with ID " + userId + " has been disabled successfully");
            response.put("status", HttpStatus.OK.value());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Failed to disable user with ID {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (SecurityException e) {
            logger.warn("Unauthorized attempt to disable user with ID {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", HttpStatus.FORBIDDEN.value());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (Exception e) {
            logger.error("Error disabling user with ID {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Server error: " + e.getMessage());
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/users/{userId}/restore")
    public ResponseEntity<Map<String, Object>> restoreUser(@PathVariable Integer userId) {
        try {
            adminService.restoreUser(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User with ID " + userId + " has been restored successfully");
            response.put("status", HttpStatus.OK.value());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to restore user with ID {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (SecurityException e) {
            logger.warn("Unauthorized attempt to restore user with ID {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", HttpStatus.FORBIDDEN.value());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (Exception e) {
            logger.error("Error restoring user with ID {}: {}", userId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Server error: " + e.getMessage());
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}