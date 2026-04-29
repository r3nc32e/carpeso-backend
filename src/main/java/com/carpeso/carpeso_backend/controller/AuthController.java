package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.request.LoginRequest;
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
import java.util.Map;

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
                    ApiResponse.success(
                            "Registration successful! Check your email for verification code.",
                            response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<?> verifyRegistration(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String ip = httpRequest.getRemoteAddr();
            AuthResponse response = authService.verifyRegistration(
                    request.get("email"),
                    request.get("otp"),
                    ip);
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Email verified! Welcome to Carpeso!", response));
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
            AuthResponse response = authService.directLogin(request, ip); //
            return ResponseEntity.ok(
                    ApiResponse.success("Login successful!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String ip = httpRequest.getRemoteAddr();
            AuthResponse response = authService.verifyOtp(
                    request.get("email"), request.get("otp"), ip);
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String ip = httpRequest.getRemoteAddr();
            authService.forgotPassword(request.get("email"), ip);
            return ResponseEntity.ok(
                    ApiResponse.success("OTP sent to your email!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody Map<String, String> request) {
        try {
            authService.resetPassword(
                    request.get("email"),
                    request.get("otp"),
                    request.get("newPassword"));
            return ResponseEntity.ok(
                    ApiResponse.success("Password reset successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-password-change-otp")
    public ResponseEntity<?> verifyPasswordChangeOtp(
            @RequestBody Map<String, String> request) {
        try {
            authService.verifyAndChangePassword(
                    request.get("email"),
                    request.get("otp"),
                    request.get("currentPassword"),
                    request.get("newPassword"));
            return ResponseEntity.ok(
                    ApiResponse.success("Password changed successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}