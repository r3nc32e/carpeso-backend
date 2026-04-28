package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.request.LoginRequest;
import com.carpeso.carpeso_backend.dto.request.RegisterRequest;
import com.carpeso.carpeso_backend.dto.response.AuthResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.enums.NotificationType;
import com.carpeso.carpeso_backend.model.enums.PaymentMode;
import com.carpeso.carpeso_backend.model.enums.Role;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.security.JwtUtil;
import com.carpeso.carpeso_backend.util.InputSanitizer;
import com.carpeso.carpeso_backend.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private InputSanitizer inputSanitizer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OtpUtil otpUtil;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        inputSanitizer.validateInput(request.getEmail(), "email");
        inputSanitizer.validateInput(request.getFirstName(), "firstName");
        inputSanitizer.validateInput(request.getLastName(), "lastName");

        if (!inputSanitizer.isValidEmail(request.getEmail())) {
            throw new RuntimeException("Invalid email format!");
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()
                && !inputSanitizer.isValidPhone(request.getPhone())) {
            throw new RuntimeException("Invalid phone number! Use 09XXXXXXXXX format.");
        }
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new RuntimeException("Email already exists!");
        }

        String otp = otpUtil.generateOtp();

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.BUYER);
        user.setFirstName(request.getFirstName());
        user.setMiddleName(request.getMiddleName());
        user.setLastName(request.getLastName());
        user.setSuffix(request.getSuffix());
        user.setPhone(request.getPhone());
        user.setCityName(request.getCityName());
        user.setBarangayName(request.getBarangayName());
        user.setStreetNo(request.getStreetNo());
        user.setLotNo(request.getLotNo());
        user.setPrivileges(new HashSet<>());
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setActive(false);
        user.setLoginAttempts(0);

        if (request.getPreferredPaymentMode() != null &&
                !request.getPreferredPaymentMode().isEmpty()) {
            try {
                user.setPreferredPaymentMode(
                        PaymentMode.valueOf(request.getPreferredPaymentMode()
                                .toUpperCase().replace(" ", "_")));
            } catch (Exception ignored) {}
        }

        userRepository.save(user);

        // Send OTP email — with better error logging
        try {
            emailService.sendRegistrationOtp(user.getEmail(), user.getFirstName(), otp);
            System.out.println("✅ OTP email sent to: " + user.getEmail() + " | OTP: " + otp);
        } catch (Exception e) {
            System.out.println("❌ Email failed: " + e.getMessage());
            e.printStackTrace();
        }

        auditLogService.log("USER_REGISTERED", user.getEmail(),
                "User", String.valueOf(user.getId()),
                "New buyer registered — awaiting email verification", "system");

        AuthResponse response = new AuthResponse();
        response.setEmail(user.getEmail());
        response.setOtpRequired(true);
        response.setOtpCode(otp); // Keep for demo/testing
        return response;
    }

    public AuthResponse verifyRegistration(String email, String otp,
                                           String ipAddress) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!otpUtil.isOtpValid(user.getOtpCode(), otp,
                user.getOtpExpiry())) {
            throw new RuntimeException("Invalid or expired OTP!");
        }

        user.setActive(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setOtpVerified(true);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole().name());

        auditLogService.log("EMAIL_VERIFIED", user.getEmail(),
                "User", String.valueOf(user.getId()),
                "Email verified — account activated", ipAddress);

        return buildAuthResponse(user, token, false);
    }

    public AuthResponse verifyOtp(String email, String otp, String ipAddress) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!otpUtil.isOtpValid(user.getOtpCode(), otp, user.getOtpExpiry())) {
            throw new RuntimeException("Invalid or expired OTP!");
        }

        user.setOtpVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        auditLogService.log("LOGIN_SUCCESS", user.getEmail(),
                "User", String.valueOf(user.getId()),
                "Successful login from " + ipAddress, ipAddress);

        return buildAuthResponse(user, token, false);
    }

    private AuthResponse buildAuthResponse(User user, String token, boolean otpRequired) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setRole(user.getRole().name());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setUserId(user.getId());
        response.setOtpRequired(otpRequired);
        if (user.getPrivileges() != null) {
            response.setPrivileges(
                    user.getPrivileges().stream()
                            .map(Enum::name)
                            .collect(Collectors.toSet()));
        }
        return response;
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }

    public void forgotPassword(String email, String ip) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found!"));

        String otp = otpUtil.generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        try {
            emailService.sendForgotPasswordOtp(user.getEmail(),
                    user.getFirstName(), otp);
        } catch (Exception e) {
            System.out.println("Email sending failed: " + e.getMessage());
        }

        auditLogService.log("FORGOT_PASSWORD", email,
                "User", String.valueOf(user.getId()),
                "Password reset OTP sent", ip);
    }

    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found!"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new RuntimeException("Invalid OTP!");
        }

        if (user.getOtpExpiry() == null ||
                LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new RuntimeException("OTP has expired!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        try {
            emailService.sendPasswordResetConfirmation(user.getEmail(),
                    user.getFirstName());
        } catch (Exception e) {
            System.out.println("Email sending failed: " + e.getMessage());
        }

        auditLogService.log("PASSWORD_RESET", email,
                "User", String.valueOf(user.getId()),
                "Password reset successfully", "system");
    }

    public void verifyAndChangePassword(String email, String otp,
                                        String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!otpUtil.isOtpValid(user.getOtpCode(), otp, user.getOtpExpiry())) {
            throw new RuntimeException("Invalid or expired OTP!");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect!");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        try {
            emailService.sendPasswordResetConfirmation(
                    user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            System.out.println("Email failed: " + e.getMessage());
        }

        auditLogService.log("PASSWORD_CHANGED", email,
                "User", String.valueOf(user.getId()),
                "Password changed via profile with OTP verification", "system");
    }
}