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
import com.carpeso.carpeso_backend.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

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

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.BUYER);
        user.setFirstName(request.getFirstName());
        user.setMiddleName(request.getMiddleName());
        user.setLastName(request.getLastName());
        user.setSuffix(request.getSuffix());
        user.setPhone(request.getPhone());
        user.setCivilStatus(request.getCivilStatus());
        user.setOccupation(request.getOccupation());
        user.setEmploymentStatus(request.getEmploymentStatus());
        user.setCityName(request.getCityName());
        user.setBarangayName(request.getBarangayName());
        user.setStreetNo(request.getStreetNo());
        user.setLotNo(request.getLotNo());
        user.setPostalCode(request.getPostalCode());
        user.setAddress(request.getAddress());
        user.setPrivileges(new HashSet<>());

        if (request.getPreferredPaymentMode() != null &&
                !request.getPreferredPaymentMode().isEmpty()) {
            try {
                user.setPreferredPaymentMode(
                        PaymentMode.valueOf(
                                request.getPreferredPaymentMode()
                                        .toUpperCase().replace(" ", "_")));
            } catch (Exception ignored) {}
        }

        userRepository.save(user);

        auditLogService.log("USER_REGISTERED", user.getEmail(),
                "User", String.valueOf(user.getId()),
                "New buyer registered: " + user.getFullName(), "system");

        String token = jwtUtil.generateToken(user.getEmail(),
                user.getRole().name());

        return buildAuthResponse(user, token, false);
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByEmail(
                        request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid email or password!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogService.log("LOGIN_FAILED", request.getEmail(),
                    "User", null, "Invalid password attempt", ipAddress);
            throw new RuntimeException("Invalid email or password!");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated!");
        }

        if (user.isSuspended()) {
            throw new RuntimeException("Account is suspended!");
        }

        // Generate OTP
        String otp = otpUtil.generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiry(otpUtil.getOtpExpiry());
        user.setOtpVerified(false);
        userRepository.save(user);

        auditLogService.log("LOGIN_OTP_SENT", user.getEmail(),
                "User", String.valueOf(user.getId()),
                "OTP sent for login verification", ipAddress);

        // Return response with OTP required
        AuthResponse response = new AuthResponse();
        response.setEmail(user.getEmail());
        response.setOtpRequired(true);
        response.setOtpCode(otp); // In production, send via email
        return response;
    }

    public AuthResponse verifyOtp(String email, String otp,
                                  String ipAddress) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!otpUtil.isOtpValid(user.getOtpCode(), otp,
                user.getOtpExpiry())) {
            throw new RuntimeException("Invalid or expired OTP!");
        }

        user.setOtpVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(),
                user.getRole().name());

        auditLogService.log("LOGIN_SUCCESS", user.getEmail(),
                "User", String.valueOf(user.getId()),
                "Successful login", ipAddress);

        return buildAuthResponse(user, token, false);
    }

    private AuthResponse buildAuthResponse(User user, String token,
                                           boolean otpRequired) {
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
}