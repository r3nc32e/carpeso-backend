package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.request.LoginRequest;
import com.carpeso.carpeso_backend.dto.request.OtpRequest;
import com.carpeso.carpeso_backend.dto.request.RegisterRequest;
import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.dto.response.AuthResponse;
import com.carpeso.carpeso_backend.dto.response.UserResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.service.AuthService;
import com.carpeso.carpeso_backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(
                    ApiResponse.success("Registration successful!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ip = httpRequest.getRemoteAddr();
            AuthResponse response = authService.login(request, ip);
            return ResponseEntity.ok(
                    ApiResponse.success("OTP sent!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody OtpRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ip = httpRequest.getRemoteAddr();
            AuthResponse response = authService.verifyOtp(
                    request.getEmail(), request.getOtp(), ip);
            return ResponseEntity.ok(
                    ApiResponse.success("Login successful!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            UserResponse response = userService.toResponse(user);
            return ResponseEntity.ok(
                    ApiResponse.success("User fetched!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(
                ApiResponse.success("Carpeso API is running!"));
    }
}