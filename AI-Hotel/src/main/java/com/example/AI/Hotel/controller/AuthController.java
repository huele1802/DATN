package com.example.AI.Hotel.controller;

import com.example.AI.Hotel.config.JwtUtil;
import com.example.AI.Hotel.dto.LoginRequest;
import com.example.AI.Hotel.dto.LoginResponse;
import com.example.AI.Hotel.dto.RegisterRequest;
import com.example.AI.Hotel.model.User;
import com.example.AI.Hotel.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;


// trả về key-value: message
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            response.put("message", "Email already exists");
            response.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        user.setRole(User.Role.USER);
        userRepository.save(user);

        response.put("message", "User registered successfully");
        response.put("status", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/login")
//    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
//        String email = authentication.getName();
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalStateException("User not found"));
//        String token = jwtUtil.generateToken(email, user.getRole().name());
//        return ResponseEntity.ok(new LoginResponse(token));
//    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            String token = jwtUtil.generateToken(email, user.getRole().name());

            response.put("status", HttpStatus.OK.value());
            response.put("message", "Login successful");
            response.put("token", token);
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            response.put("status", HttpStatus.UNAUTHORIZED.value());
            response.put("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }


    // api tạo admin
//    @PostMapping("/create-admin")
//    public ResponseEntity<String> createAdmin(@Valid @RequestBody RegisterRequest request, @RequestParam String secret) {
//        if (!adminCreationSecret.equals(secret)) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin creation secret");
//        }
//
//        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
//            return ResponseEntity.badRequest().body("Email already exists");
//        }
//
//        User user = new User();
//        user.setEmail(request.getEmail());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setFullName(request.getFullName());
//        user.setPhoneNumber(request.getPhoneNumber());
//        user.setDateOfBirth(request.getDateOfBirth());
//        user.setAddress(request.getAddress());
//        user.setRole(User.Role.ADMIN); // Đặt vai trò là ADMIN
//        userRepository.save(user);
//
//        return ResponseEntity.ok("Admin account created successfully");
//    }
//
    @GetMapping("/oauth2/success")
    public ResponseEntity<LoginResponse> oauth2LoginSuccess(@AuthenticationPrincipal OAuth2User principal) {
        String googleId = principal.getAttribute("sub");
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        User user = userRepository.findByGoogleId(googleId).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setAvatarUrl(picture);
            newUser.setGoogleId(googleId);
            newUser.setRole(User.Role.USER);
            return userRepository.save(newUser);
        });

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
