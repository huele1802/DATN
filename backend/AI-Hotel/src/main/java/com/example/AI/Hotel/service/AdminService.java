package com.example.AI.Hotel.service;

import com.example.AI.Hotel.model.User;
import com.example.AI.Hotel.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;

    @Autowired
    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void disableUser(Integer userId) {
        // Lấy thông tin user hiện tại từ SecurityContext
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin not found"));

        // Kiểm tra quyền admin từ authorities
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));

        if (!isAdmin) {
            logger.warn("User {} attempted to disable a user but lacks ADMIN role", adminEmail);
            throw new SecurityException("Only admins can disable user accounts");
        }

        // Kiểm tra user tồn tại
        if (!userRepository.existsByIdAndNotDeleted(userId)) {
            logger.warn("User with ID {} not found or already deleted", userId);
            throw new IllegalArgumentException("User with ID " + userId + " not found or already deleted");
        }

        // Vô hiệu hóa tài khoản
        userRepository.softDeleteById(userId);
        logger.info("Admin {} disabled user with ID {}", adminEmail, userId);
    }

    @Transactional
    public void restoreUser(Integer userId) {
        // Lấy thông tin user hiện tại từ SecurityContext
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin not found"));

        // Kiểm tra quyền admin từ authorities
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));

        if (!isAdmin) {
            logger.warn("User {} attempted to restore a user but lacks ADMIN role", adminEmail);
            throw new SecurityException("Only admins can restore user accounts");
        }

        // Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));

        if (!user.isDeleted()) {
            logger.warn("User with ID {} is not deleted, no need to restore", userId);
            throw new IllegalStateException("User with ID " + userId + " is not deleted");
        }

        // Khôi phục tài khoản
        userRepository.restoreById(userId);
        logger.info("Admin {} restored user with ID {}", adminEmail, userId);
    }
}