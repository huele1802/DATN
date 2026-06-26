package com.example.AI.Hotel.service;

import com.example.AI.Hotel.model.User;
import com.example.AI.Hotel.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void updatePassword(String email, String oldPassword, String newPassword) {
        // Lấy thông tin user hiện tại
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // Kiểm tra tài khoản không bị vô hiệu hóa
        if (user.isDeleted()) {
            logger.warn("User {} attempted to update password but account is disabled", email);
            throw new SecurityException("Account is disabled");
        }

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            logger.warn("User {} entered incorrect old password", email);
            throw new SecurityException("Old password is incorrect");
        }

        // Kiểm tra mật khẩu mới không trùng với mật khẩu cũ
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            logger.warn("User {} attempted to set new password same as old password", email);
            throw new IllegalArgumentException("New password must be different from old password");
        }

        // Mã hóa mật khẩu mới
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);

        // Lưu thay đổi
        userRepository.save(user);
        logger.info("User {} updated password successfully", email);
    }
}
