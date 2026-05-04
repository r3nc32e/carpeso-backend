package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.response.UserResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.enums.AdminPrivilege;
import com.carpeso.carpeso_backend.model.enums.NotificationType;
import com.carpeso.carpeso_backend.model.enums.Role;
import com.carpeso.carpeso_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogService auditLogService;
    @Autowired private NotificationService notificationService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    public List<UserResponse> getAllBuyers() {
        return userRepository.findByRole(Role.BUYER)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<UserResponse> getAllAdmins() {
        return userRepository.findByRole(Role.ADMIN)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found!")));
    }

    // ─── Warn user / buyer ────────────────────────────────────────────────────

    public void warnUser(Long userId, String reason, String performedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setWarningCount(user.getWarningCount() + 1);

        if (user.getWarningCount() >= 3) {
            user.setSuspended(true);
            notificationService.send(user,
                    "Account Suspended",
                    "Your account has been suspended due to multiple violations.",
                    NotificationType.ACCOUNT_WARNING, null);
            try {
                emailService.sendAccountSuspension(
                        user.getEmail(), user.getFirstName(),
                        "Multiple warnings received: " + reason,
                        "Permanent until reviewed");
            } catch (Exception e) {
                System.out.println("Email failed: " + e.getMessage());
            }
        } else {
            notificationService.send(user,
                    "Warning Issued",
                    "You have received a warning: " + reason,
                    NotificationType.ACCOUNT_WARNING, null);
            try {
                emailService.sendWarningNotification(
                        user.getEmail(), user.getFirstName(),
                        reason, user.getWarningCount());
            } catch (Exception e) {
                System.out.println("Email failed: " + e.getMessage());
            }
        }

        userRepository.save(user);
        auditLogService.log("USER_WARNED", performedBy,
                "User", String.valueOf(userId),
                "Warning issued: " + reason, "system");
    }

    // ─── Suspend user / buyer ─────────────────────────────────────────────────

    public void suspendUser(Long userId, String reason,
                            int durationDays, String performedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setSuspended(true);
        userRepository.save(user);

        String durationLabel = durationDays == -1
                ? "Permanent" : durationDays + " day(s)";

        try {
            emailService.sendAccountSuspension(
                    user.getEmail(), user.getFirstName(), reason, durationLabel);
        } catch (Exception e) {
            System.out.println("Email failed: " + e.getMessage());
        }

        notificationService.send(user,
                "Account Suspended",
                "Your account has been suspended. Reason: " + reason,
                NotificationType.ACCOUNT_WARNING, null);

        auditLogService.log("USER_SUSPENDED", performedBy,
                "User", String.valueOf(userId),
                "Suspended: " + reason + " | Duration: " + durationLabel, "system");
    }

    // ─── Unsuspend user / buyer ───────────────────────────────────────────────

    public void unsuspendUser(Long userId, String performedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setSuspended(false);
        user.setWarningCount(0);
        userRepository.save(user);

        notificationService.send(user,
                "Account Reinstated",
                "Your account has been reinstated. Welcome back!",
                NotificationType.ACCOUNT_WARNING, null);
        try {
            emailService.sendAccountReinstatement(
                    user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            System.out.println("Email failed: " + e.getMessage());
        }

        auditLogService.log("USER_UNSUSPENDED", performedBy,
                "User", String.valueOf(userId),
                "User unsuspended", "system");
    }

    // ─── Delete user ──────────────────────────────────────────────────────────

    public void deleteUser(Long userId, String performedBy) {
        userRepository.deleteById(userId);
        auditLogService.log("USER_DELETED", performedBy,
                "User", String.valueOf(userId),
                "User deleted", "system");
    }

    // ─── Update admin privileges ──────────────────────────────────────────────

    public void updateAdminPrivileges(Long adminId,
                                      Set<AdminPrivilege> privileges, String performedBy) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found!"));
        if (admin.getRole() != Role.ADMIN)
            throw new RuntimeException("User is not an admin!");
        admin.setPrivileges(privileges);
        userRepository.save(admin);
        auditLogService.log("PRIVILEGES_UPDATED", performedBy,
                "User", String.valueOf(adminId),
                "Privileges updated: " + privileges, "system");
    }

    // ─── Create admin — original signature (backward compat) ─────────────────

    public User createAdmin(String email, String password,
                            String firstName, String lastName,
                            Set<AdminPrivilege> privileges,
                            String performedBy,
                            PasswordEncoder encoder) {
        return createAdmin(email, password, firstName, lastName,
                null, null, null, privileges, performedBy, encoder);
    }

    // ─── Create admin — full signature with optional fields ──────────────────

    public User createAdmin(String email, String password,
                            String firstName, String lastName,
                            String middleName, String suffix, String phone,
                            Set<AdminPrivilege> privileges,
                            String performedBy,
                            PasswordEncoder encoder) {
        if (email == null || email.isBlank())
            throw new RuntimeException("Email is required!");
        if (userRepository.existsByEmail(email.toLowerCase()))
            throw new RuntimeException("Email already exists!");

        User admin = new User();
        admin.setEmail(email.toLowerCase());
        admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        if (middleName != null) admin.setMiddleName(middleName);
        if (suffix != null)     admin.setSuffix(suffix);
        if (phone != null)      admin.setPhone(phone);
        admin.setPrivileges(privileges != null ? privileges : new HashSet<>());
        admin.setActive(true);        // Admins are active immediately — no OTP needed
        admin.setSuspended(false);
        admin.setWarningCount(0);
        admin.setLoginAttempts(0);

        userRepository.save(admin);
        auditLogService.log("ADMIN_CREATED", performedBy,
                "User", String.valueOf(admin.getId()),
                "Sub-admin created: " + email, "system");
        return admin;
    }

    // ─── Update profile fields ────────────────────────────────────────────────

    public void updateProfile(User user, Map<String, String> request) {
        if (request.get("firstName") != null)
            user.setFirstName(request.get("firstName"));
        if (request.get("lastName") != null)
            user.setLastName(request.get("lastName"));
        if (request.get("middleName") != null)
            user.setMiddleName(request.get("middleName"));
        if (request.get("suffix") != null)
            user.setSuffix(request.get("suffix"));
        if (request.get("phone") != null)
            user.setPhone(request.get("phone"));
        if (request.get("cityName") != null)
            user.setCityName(request.get("cityName"));
        if (request.get("barangayName") != null)
            user.setBarangayName(request.get("barangayName"));
        if (request.get("streetNo") != null)
            user.setStreetNo(request.get("streetNo"));
        userRepository.save(user);
    }

    // ─── Change password (used by buyers via BuyerController) ────────────────

    public void changePassword(User user, String currentPassword,
                               String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new RuntimeException("Current password is incorrect!");
        if (newPassword == null || newPassword.length() < 8)
            throw new RuntimeException("New password must be at least 8 characters!");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─── toResponse ───────────────────────────────────────────────────────────

    public UserResponse toResponse(User user) {
        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setFirstName(user.getFirstName());
        res.setMiddleName(user.getMiddleName());
        res.setLastName(user.getLastName());
        res.setSuffix(user.getSuffix());
        res.setPhone(user.getPhone());
        res.setCivilStatus(user.getCivilStatus());
        res.setOccupation(user.getOccupation());
        res.setEmploymentStatus(user.getEmploymentStatus());
        res.setAddress(user.getAddress());
        res.setCityName(user.getCityName());
        res.setBarangayName(user.getBarangayName());
        res.setRole(user.getRole().name());
        res.setActive(user.isActive());
        res.setSuspended(user.isSuspended());
        res.setWarningCount(user.getWarningCount());
        res.setCreatedAt(user.getCreatedAt());
        res.setLastLogin(user.getLastLogin());
        res.setPrimaryIdUrl(user.getPrimaryIdUrl());
        res.setSecondaryIdUrl(user.getSecondaryIdUrl());
        res.setStreetNo(user.getStreetNo());
        if (user.getPrivileges() != null) {
            res.setPrivileges(user.getPrivileges().stream()
                    .map(Enum::name).collect(Collectors.toSet()));
        }
        return res;
    }
}