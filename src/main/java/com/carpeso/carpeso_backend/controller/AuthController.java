package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.request.LoginRequest;
import com.carpeso.carpeso_backend.dto.request.RegisterRequest;
import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.dto.response.AuthResponse;
import com.carpeso.carpeso_backend.dto.response.UserResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.service.AuditLogService;
import com.carpeso.carpeso_backend.service.AuthService;
import com.carpeso.carpeso_backend.service.EmailService;
import com.carpeso.carpeso_backend.service.UserService;
import com.carpeso.carpeso_backend.util.OtpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditLogService auditLogService;
    @Autowired private EmailService emailService;
    @Autowired private OtpUtil otpUtil;

    // ─── REGISTER (Buyer) ─────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Registration successful! Check your email for verification code.",
                    response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<?> verifyRegistration(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.verifyRegistration(
                    request.get("email"), request.get("otp"),
                    httpRequest.getRemoteAddr());
            return ResponseEntity.ok(ApiResponse.success(
                    "Email verified! Welcome to Carpeso!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.directLogin(request,
                    httpRequest.getRemoteAddr());
            return ResponseEntity.ok(ApiResponse.success("Login successful!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.verifyOtp(
                    request.get("email"), request.get("otp"),
                    httpRequest.getRemoteAddr());
            return ResponseEntity.ok(ApiResponse.success("Login successful!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── CURRENT USER ─────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            return ResponseEntity.ok(ApiResponse.success("User fetched!",
                    userService.toResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(ApiResponse.success("Carpeso API is running!"));
    }

    // ─── FORGOT / RESET PASSWORD (unauthenticated) ────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            authService.forgotPassword(request.get("email"),
                    httpRequest.getRemoteAddr());
            return ResponseEntity.ok(ApiResponse.success("OTP sent to your email!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            authService.resetPassword(
                    request.get("email"),
                    request.get("otp"),
                    request.get("newPassword"));
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── CHANGE PASSWORD — authenticated ─────────────────────────────────────
    // Requires: currentPassword + OTP
    //
    // Flow:
    //   Step 1 → POST /api/auth/request-password-otp   (logged in, no body)
    //   Step 2 → POST /api/auth/verify-password-change-otp
    //              { email, otp, currentPassword, newPassword }

    @PostMapping("/request-password-otp")
    public ResponseEntity<?> requestPasswordChangeOtp(Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());

            String otp = otpUtil.generateOtp();
            user.setOtpCode(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            try {
                emailService.sendForgotPasswordOtp(
                        user.getEmail(), user.getFirstName(), otp);
            } catch (Exception e) {
                System.out.println("OTP email failed: " + e.getMessage());
            }

            auditLogService.log("PASSWORD_CHANGE_OTP_SENT", user.getEmail(),
                    "User", String.valueOf(user.getId()),
                    "OTP sent for password change", "system");

            return ResponseEntity.ok(ApiResponse.success("OTP sent to your email!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── CHANGE EMAIL — authenticated ─────────────────────────────────────────
    // Requires: currentPassword + OTP
    //
    // Flow:
    //   Step 1 → POST /api/auth/request-email-otp  (logged in, no body)
    //   Step 2 → PUT  /api/auth/change-email
    //              { currentPassword, otp, newEmail }

    @PostMapping("/request-email-otp")
    public ResponseEntity<?> requestEmailChangeOtp(Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());

            String otp = otpUtil.generateOtp();
            user.setOtpCode(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            try {
                emailService.sendForgotPasswordOtp(
                        user.getEmail(), user.getFirstName(), otp);
            } catch (Exception e) {
                System.out.println("OTP email failed: " + e.getMessage());
            }

            auditLogService.log("EMAIL_CHANGE_OTP_SENT", user.getEmail(),
                    "User", String.valueOf(user.getId()),
                    "OTP sent for email change", "system");

            return ResponseEntity.ok(ApiResponse.success(
                    "OTP sent to your current email!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/change-email")
    public ResponseEntity<?> changeEmail(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());

            String currentPassword = request.get("currentPassword");
            String otp             = request.get("otp");
            String newEmail        = request.get("newEmail");

            if (currentPassword == null || currentPassword.isBlank())
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is required!"));

            if (!passwordEncoder.matches(currentPassword, user.getPassword()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is incorrect!"));

            if (!otpUtil.isOtpValid(user.getOtpCode(), otp, user.getOtpExpiry()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid or expired OTP!"));

            if (newEmail == null || newEmail.isBlank())
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("New email is required!"));

            newEmail = newEmail.toLowerCase().trim();

            if (!newEmail.equals(user.getEmail())
                    && userRepository.existsByEmail(newEmail))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email already in use!"));

            user.setEmail(newEmail);
            user.setOtpCode(null);
            user.setOtpExpiry(null);
            userRepository.save(user);

            auditLogService.log("EMAIL_CHANGED", user.getEmail(),
                    "User", String.valueOf(user.getId()),
                    "Email changed to: " + newEmail, "system");

            return ResponseEntity.ok(ApiResponse.success(
                    "Email changed successfully! Please login again with your new email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── UPDATE PROFILE FIELDS — no OTP, no current password ─────────────────
    // Only non-sensitive fields: firstName, lastName, middleName, suffix,
    // phone, cityName, barangayName, streetNo
    // Password → use /request-password-otp then /verify-password-change-otp
    // Email    → use /request-email-otp then /change-email

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            userService.updateProfile(user, request);
            User updated = authService.getCurrentUser(auth.getName());

            auditLogService.log("PROFILE_UPDATED", user.getEmail(),
                    "User", String.valueOf(user.getId()),
                    "Profile fields updated", "system");

            return ResponseEntity.ok(ApiResponse.success("Profile updated!",
                    userService.toResponse(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}