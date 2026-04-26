package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.response.UserResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.enums.AdminPrivilege;
import com.carpeso.carpeso_backend.model.enums.Role;
import com.carpeso.carpeso_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    public void warnUser(Long userId, String reason, String performedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setWarningCount(user.getWarningCount() + 1);

        if (user.getWarningCount() >= 3) {
            user.setSuspended(true);
            notificationService.send(user,
                    "Account Suspended",
                    "Your account has been suspended due to multiple violations.",
                    com.carpeso.carpeso_backend.model.enums.NotificationType.ACCOUNT_WARNING,
                    null);
        } else {
            notificationService.send(user,
                    "Warning Issued",
                    "You have received a warning: " + reason,
                    com.carpeso.carpeso_backend.model.enums.NotificationType.ACCOUNT_WARNING,
                    null);
        }

        userRepository.save(user);
        auditLogService.log("USER_WARNED", performedBy,
                "User", String.valueOf(userId),
                "Warning issued: " + reason, "system");
    }

    public void suspendUser(Long userId, String performedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setSuspended(true);
        userRepository.save(user);
        auditLogService.log("USER_SUSPENDED", performedBy,
                "User", String.valueOf(userId),
                "User suspended", "system");
    }

    public void unsuspendUser(Long userId, String performedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setSuspended(false);
        user.setWarningCount(0);
        userRepository.save(user);
        auditLogService.log("USER_UNSUSPENDED", performedBy,
                "User", String.valueOf(userId),
                "User unsuspended", "system");
    }

    public void deleteUser(Long userId, String performedBy) {
        userRepository.deleteById(userId);
        auditLogService.log("USER_DELETED", performedBy,
                "User", String.valueOf(userId),
                "User deleted", "system");
    }

    public void updateAdminPrivileges(Long adminId,
                                      Set<AdminPrivilege> privileges, String performedBy) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found!"));
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("User is not an admin!");
        }
        admin.setPrivileges(privileges);
        userRepository.save(admin);
        auditLogService.log("PRIVILEGES_UPDATED", performedBy,
                "User", String.valueOf(adminId),
                "Privileges updated: " + privileges, "system");
    }

    public User createAdmin(String email, String password,
                            String firstName, String lastName,
                            Set<AdminPrivilege> privileges,
                            String performedBy,
                            PasswordEncoder encoder) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists!");
        }
        User admin = new User();
        admin.setEmail(email.toLowerCase());
        admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setPrivileges(privileges);
        admin.setActive(true);
        admin.setLoginAttempts(0);
        userRepository.save(admin);
        auditLogService.log("ADMIN_CREATED", performedBy,
                "User", String.valueOf(admin.getId()),
                "Admin created: " + email, "system");
        return admin;
    }

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

    public void changePassword(User user, String currentPassword,
                               String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect!");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException(
                    "New password must be at least 8 characters!");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

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
        if (user.getPrivileges() != null) {
            res.setPrivileges(user.getPrivileges().stream()
                    .map(Enum::name).collect(Collectors.toSet()));
        }
        return res;
    }
}