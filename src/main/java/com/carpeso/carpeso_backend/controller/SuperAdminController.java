package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.enums.AdminPrivilege;
import com.carpeso.carpeso_backend.model.enums.Role;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.service.AuditLogService;
import com.carpeso.carpeso_backend.service.AuthService;
import com.carpeso.carpeso_backend.service.EmailService;
import com.carpeso.carpeso_backend.service.UserService;
import com.carpeso.carpeso_backend.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/superadmin")
@CrossOrigin(origins = "*")
public class SuperAdminController {

    @Autowired private UserService userService;
    @Autowired private EmailService emailService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OtpUtil otpUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthService authService;

    // ═══════════════════════════════════════════════════════════════════════════
    // SUB-ADMIN MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    // ─── List all sub-admins ──────────────────────────────────────────────────

    @GetMapping("/admins")
    public ResponseEntity<?> getAllAdmins() {
        return ResponseEntity.ok(ApiResponse.success(
                "Admins fetched!", userService.getAllAdmins()));
    }

    // ─── View one sub-admin ───────────────────────────────────────────────────

    @GetMapping("/admins/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable Long id) {
        try {
            User admin = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin not found!"));
            if (admin.getRole() != Role.ADMIN)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a sub-admin!"));
            return ResponseEntity.ok(ApiResponse.success(
                    "Admin fetched!", userService.toResponse(admin)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Register sub-admin ───────────────────────────────────────────────────
    // Like buyer registration — superadmin fills in the details.
    // No OTP needed since superadmin is already authenticated.
    // Admin is active immediately.

    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String email      = (String) request.get("email");
            String password   = (String) request.get("password");
            String firstName  = (String) request.get("firstName");
            String lastName   = (String) request.get("lastName");
            String middleName = (String) request.get("middleName");
            String suffix     = (String) request.get("suffix");
            String phone      = (String) request.get("phone");

            if (email == null || email.isBlank())
                return ResponseEntity.badRequest().body(ApiResponse.error("Email is required!"));
            if (password == null || password.length() < 8)
                return ResponseEntity.badRequest().body(ApiResponse.error("Password must be at least 8 characters!"));
            if (firstName == null || firstName.isBlank())
                return ResponseEntity.badRequest().body(ApiResponse.error("First name is required!"));
            if (lastName == null || lastName.isBlank())
                return ResponseEntity.badRequest().body(ApiResponse.error("Last name is required!"));

            @SuppressWarnings("unchecked")
            List<String> privilegeList = (List<String>) request.get("privileges");
            Set<AdminPrivilege> privileges = privilegeList == null
                    ? new HashSet<>()
                    : privilegeList.stream()
                      .map(AdminPrivilege::valueOf)
                      .collect(Collectors.toSet());

            User admin = userService.createAdmin(
                    email, password, firstName, lastName,
                    middleName, suffix, phone,
                    privileges, auth.getName(), passwordEncoder);

            try {
                emailService.sendAdminCreated(
                        admin.getEmail(), admin.getFirstName(), password, "ADMIN");
            } catch (Exception e) {
                System.out.println("Email notification failed: " + e.getMessage());
            }

            return ResponseEntity.ok(ApiResponse.success(
                    "Sub-admin registered successfully!", userService.toResponse(admin)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Edit sub-admin profile fields (no OTP — superadmin authority is enough)
    // Only non-sensitive fields: name, phone.
    // To change a sub-admin's email → use /admins/{id}/change-email
    // To reset a sub-admin's password → use /admins/{id}/reset-password

    @PutMapping("/admins/{id}")
    public ResponseEntity<?> updateAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User admin = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin not found!"));
            if (admin.getRole() != Role.ADMIN)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a sub-admin!"));

            if (request.containsKey("firstName") && !request.get("firstName").isBlank())
                admin.setFirstName(request.get("firstName"));
            if (request.containsKey("lastName") && !request.get("lastName").isBlank())
                admin.setLastName(request.get("lastName"));
            if (request.containsKey("middleName"))
                admin.setMiddleName(request.get("middleName"));
            if (request.containsKey("suffix"))
                admin.setSuffix(request.get("suffix"));
            if (request.containsKey("phone"))
                admin.setPhone(request.get("phone"));

            userRepository.save(admin);

            auditLogService.log("ADMIN_PROFILE_UPDATED", auth.getName(),
                    "User", String.valueOf(id),
                    "Sub-admin profile fields updated", "system");

            return ResponseEntity.ok(ApiResponse.success(
                    "Admin details updated!", userService.toResponse(admin)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Reset sub-admin email (superadmin does it — requires OTP sent to
    //     superadmin's own email as confirmation, plus current password of superadmin)
    //
    // Flow:
    //   Step 1 → POST /api/superadmin/send-otp     (generates OTP for superadmin)
    //   Step 2 → PUT  /api/superadmin/admins/{id}/change-email
    //               { superadminPassword, otp, newEmail }

    @PutMapping("/admins/{id}/change-email")
    public ResponseEntity<?> changeAdminEmail(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User superAdmin = authService.getCurrentUser(auth.getName());
            if (superAdmin.getRole() != Role.SUPERADMIN)
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied!"));

            User admin = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin not found!"));
            if (admin.getRole() != Role.ADMIN)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a sub-admin!"));

            String superadminPassword = request.get("superadminPassword");
            String otp                = request.get("otp");
            String newEmail           = request.get("newEmail");

            if (superadminPassword == null || superadminPassword.isBlank())
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Your (superadmin) current password is required!"));

            if (!passwordEncoder.matches(superadminPassword, superAdmin.getPassword()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Superadmin password is incorrect!"));

            if (!otpUtil.isOtpValid(superAdmin.getOtpCode(), otp, superAdmin.getOtpExpiry()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid or expired OTP!"));

            if (newEmail == null || newEmail.isBlank())
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("New email is required!"));

            newEmail = newEmail.toLowerCase().trim();

            if (!newEmail.equals(admin.getEmail())
                    && userRepository.existsByEmail(newEmail))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email already in use!"));

            admin.setEmail(newEmail);
            // Clear OTP from superadmin after use
            superAdmin.setOtpCode(null);
            superAdmin.setOtpExpiry(null);
            userRepository.save(admin);
            userRepository.save(superAdmin);

            auditLogService.log("ADMIN_EMAIL_CHANGED", auth.getName(),
                    "User", String.valueOf(id),
                    "Sub-admin email changed to: " + newEmail, "system");

            return ResponseEntity.ok(ApiResponse.success(
                    "Admin email changed! They must login again with the new email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Reset sub-admin password (superadmin does it — requires OTP + superadmin password)
    //
    // Flow:
    //   Step 1 → POST /api/superadmin/send-otp
    //   Step 2 → PUT  /api/superadmin/admins/{id}/reset-password
    //               { superadminPassword, otp, newPassword }

    @PutMapping("/admins/{id}/reset-password")
    public ResponseEntity<?> resetAdminPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User superAdmin = authService.getCurrentUser(auth.getName());
            if (superAdmin.getRole() != Role.SUPERADMIN)
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied!"));

            User admin = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin not found!"));
            if (admin.getRole() != Role.ADMIN)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a sub-admin!"));

            String superadminPassword = request.get("superadminPassword");
            String otp                = request.get("otp");
            String newPassword        = request.get("newPassword");

            if (superadminPassword == null || superadminPassword.isBlank())
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Your (superadmin) current password is required!"));

            if (!passwordEncoder.matches(superadminPassword, superAdmin.getPassword()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Superadmin password is incorrect!"));

            if (!otpUtil.isOtpValid(superAdmin.getOtpCode(), otp, superAdmin.getOtpExpiry()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid or expired OTP!"));

            if (newPassword == null || newPassword.length() < 8)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("New password must be at least 8 characters!"));

            admin.setPassword(passwordEncoder.encode(newPassword));
            superAdmin.setOtpCode(null);
            superAdmin.setOtpExpiry(null);
            userRepository.save(admin);
            userRepository.save(superAdmin);

            try {
                emailService.sendAdminCreated(
                        admin.getEmail(), admin.getFirstName(), newPassword, "ADMIN");
            } catch (Exception e) {
                System.out.println("Email failed: " + e.getMessage());
            }

            auditLogService.log("ADMIN_PASSWORD_RESET", auth.getName(),
                    "User", String.valueOf(id),
                    "Sub-admin password reset by superadmin", "system");

            return ResponseEntity.ok(ApiResponse.success(
                    "Admin password reset successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Update sub-admin privileges ──────────────────────────────────────────

    @PutMapping("/admins/{id}/privileges")
    public ResponseEntity<?> updatePrivileges(
            @PathVariable Long id,
            @RequestBody Set<String> privileges,
            Authentication auth) {
        try {
            Set<AdminPrivilege> adminPrivileges = privileges.stream()
                    .map(AdminPrivilege::valueOf)
                    .collect(Collectors.toSet());
            userService.updateAdminPrivileges(id, adminPrivileges, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Privileges updated!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Warn sub-admin (auto-suspends at 3 warnings) ────────────────────────

    @PutMapping("/admins/{id}/warn")
    public ResponseEntity<?> warnAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User admin = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin not found!"));
            if (admin.getRole() != Role.ADMIN)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a sub-admin!"));

            String reason = request.getOrDefault("reason", "Policy violation");
            admin.setWarningCount(admin.getWarningCount() + 1);

            if (admin.getWarningCount() >= 3) {
                admin.setSuspended(true);
                try {
                    emailService.sendAccountSuspension(
                            admin.getEmail(), admin.getFirstName(),
                            "Multiple warnings received: " + reason,
                            "Permanent until reviewed");
                } catch (Exception e) {
                    System.out.println("Email failed: " + e.getMessage());
                }
                auditLogService.log("ADMIN_AUTO_SUSPENDED", auth.getName(),
                        "User", String.valueOf(id),
                        "Admin auto-suspended after 3 warnings: " + reason, "system");
            } else {
                try {
                    emailService.sendWarningNotification(
                            admin.getEmail(), admin.getFirstName(),
                            reason, admin.getWarningCount());
                } catch (Exception e) {
                    System.out.println("Email failed: " + e.getMessage());
                }
                auditLogService.log("ADMIN_WARNED", auth.getName(),
                        "User", String.valueOf(id),
                        "Warning #" + admin.getWarningCount() + ": " + reason, "system");
            }

            userRepository.save(admin);
            return ResponseEntity.ok(ApiResponse.success(
                    "Warning issued! Warning count: " + admin.getWarningCount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Suspend sub-admin ────────────────────────────────────────────────────

    @PutMapping("/admins/{id}/suspend")
    public ResponseEntity<?> suspendAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            User admin = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin not found!"));
            if (admin.getRole() != Role.ADMIN)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a sub-admin!"));

            String reason      = (String) request.getOrDefault("reason", "Policy violation");
            int    durationDays = Integer.parseInt(
                    String.valueOf(request.getOrDefault("durationDays", 30)));

            admin.setSuspended(true);
            userRepository.save(admin);

            String durationLabel = durationDays == -1 ? "Permanent" : durationDays + " day(s)";

            try {
                emailService.sendAccountSuspension(
                        admin.getEmail(), admin.getFirstName(), reason, durationLabel);
            } catch (Exception e) {
                System.out.println("Email failed: " + e.getMessage());
            }

            auditLogService.log("ADMIN_SUSPENDED", auth.getName(),
                    "User", String.valueOf(id),
                    "Suspended: " + reason + " | Duration: " + durationLabel, "system");

            return ResponseEntity.ok(ApiResponse.success("Admin suspended!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Unsuspend sub-admin ──────────────────────────────────────────────────

    @PutMapping("/admins/{id}/unsuspend")
    public ResponseEntity<?> unsuspendAdmin(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User admin = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin not found!"));
            if (admin.getRole() != Role.ADMIN)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a sub-admin!"));

            admin.setSuspended(false);
            admin.setWarningCount(0);
            userRepository.save(admin);

            try {
                emailService.sendAccountReinstatement(
                        admin.getEmail(), admin.getFirstName());
            } catch (Exception e) {
                System.out.println("Email failed: " + e.getMessage());
            }

            auditLogService.log("ADMIN_UNSUSPENDED", auth.getName(),
                    "User", String.valueOf(id),
                    "Admin account reinstated", "system");

            return ResponseEntity.ok(ApiResponse.success("Admin unsuspended!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Delete sub-admin (prefer suspend over delete) ───────────────────────

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deleteAdmin(
            @PathVariable Long id,
            Authentication auth) {
        try {
            userService.deleteUser(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Admin deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Audit logs ───────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAllLogs() {
        return ResponseEntity.ok(ApiResponse.success(
                "Logs fetched!", auditLogService.getAllLogs()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUPERADMIN OWN ACCOUNT MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    // ─── Edit own profile fields (name, phone) — no OTP ──────────────────────

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User superAdmin = authService.getCurrentUser(auth.getName());
            if (superAdmin.getRole() != Role.SUPERADMIN)
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied!"));

            if (request.containsKey("firstName") && !request.get("firstName").isBlank())
                superAdmin.setFirstName(request.get("firstName"));
            if (request.containsKey("lastName") && !request.get("lastName").isBlank())
                superAdmin.setLastName(request.get("lastName"));
            if (request.containsKey("middleName"))
                superAdmin.setMiddleName(request.get("middleName"));
            if (request.containsKey("suffix"))
                superAdmin.setSuffix(request.get("suffix"));
            if (request.containsKey("phone"))
                superAdmin.setPhone(request.get("phone"));

            userRepository.save(superAdmin);

            auditLogService.log("SUPERADMIN_PROFILE_UPDATED", auth.getName(),
                    "User", String.valueOf(superAdmin.getId()),
                    "SuperAdmin profile fields updated", "system");

            return ResponseEntity.ok(ApiResponse.success(
                    "Profile updated!", userService.toResponse(superAdmin)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Send OTP to superadmin's own email ───────────────────────────────────
    // Used before changing own password OR changing own email OR
    // resetting a sub-admin's password/email.

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(Authentication auth) {
        try {
            User superAdmin = authService.getCurrentUser(auth.getName());
            if (superAdmin.getRole() != Role.SUPERADMIN)
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied!"));

            String otp = otpUtil.generateOtp();
            superAdmin.setOtpCode(otp);
            superAdmin.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(superAdmin);

            try {
                emailService.sendForgotPasswordOtp(
                        superAdmin.getEmail(), superAdmin.getFirstName(), otp);
            } catch (Exception e) {
                System.out.println("OTP email failed: " + e.getMessage());
            }

            auditLogService.log("SUPERADMIN_OTP_SENT", auth.getName(),
                    "User", String.valueOf(superAdmin.getId()),
                    "OTP sent for sensitive action", "system");

            return ResponseEntity.ok(ApiResponse.success("OTP sent to your email!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Change own password — requires currentPassword + OTP ────────────────
    // Flow:
    //   Step 1 → POST /api/superadmin/send-otp
    //   Step 2 → PUT  /api/superadmin/change-password
    //               { currentPassword, otp, newPassword }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User superAdmin = authService.getCurrentUser(auth.getName());
            if (superAdmin.getRole() != Role.SUPERADMIN)
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied!"));

            String currentPassword = request.get("currentPassword");
            String otp             = request.get("otp");
            String newPassword     = request.get("newPassword");

            if (currentPassword == null || currentPassword.isBlank())
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is required!"));

            if (!passwordEncoder.matches(currentPassword, superAdmin.getPassword()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is incorrect!"));

            if (!otpUtil.isOtpValid(superAdmin.getOtpCode(), otp, superAdmin.getOtpExpiry()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid or expired OTP!"));

            if (newPassword == null || newPassword.length() < 8)
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("New password must be at least 8 characters!"));

            superAdmin.setPassword(passwordEncoder.encode(newPassword));
            superAdmin.setOtpCode(null);
            superAdmin.setOtpExpiry(null);
            userRepository.save(superAdmin);

            auditLogService.log("SUPERADMIN_PASSWORD_CHANGED", auth.getName(),
                    "User", String.valueOf(superAdmin.getId()),
                    "SuperAdmin changed own password", "system");

            return ResponseEntity.ok(ApiResponse.success("Password changed successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Change own email — requires currentPassword + OTP ───────────────────
    // Flow:
    //   Step 1 → POST /api/superadmin/send-otp
    //   Step 2 → PUT  /api/superadmin/change-email
    //               { currentPassword, otp, newEmail }

    @PutMapping("/change-email")
    public ResponseEntity<?> changeEmail(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User superAdmin = authService.getCurrentUser(auth.getName());
            if (superAdmin.getRole() != Role.SUPERADMIN)
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied!"));

            String currentPassword = request.get("currentPassword");
            String otp             = request.get("otp");
            String newEmail        = request.get("newEmail");

            if (currentPassword == null || currentPassword.isBlank())
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is required!"));

            if (!passwordEncoder.matches(currentPassword, superAdmin.getPassword()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is incorrect!"));

            if (!otpUtil.isOtpValid(superAdmin.getOtpCode(), otp, superAdmin.getOtpExpiry()))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid or expired OTP!"));

            if (newEmail == null || newEmail.isBlank())
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("New email is required!"));

            newEmail = newEmail.toLowerCase().trim();

            if (!newEmail.equals(superAdmin.getEmail())
                    && userRepository.existsByEmail(newEmail))
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email already in use!"));

            superAdmin.setEmail(newEmail);
            superAdmin.setOtpCode(null);
            superAdmin.setOtpExpiry(null);
            userRepository.save(superAdmin);

            auditLogService.log("SUPERADMIN_EMAIL_CHANGED", auth.getName(),
                    "User", String.valueOf(superAdmin.getId()),
                    "SuperAdmin email changed to: " + newEmail, "system");

            return ResponseEntity.ok(ApiResponse.success(
                    "Email changed successfully! Please login again with your new email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Keep legacy OTP send alias for backward compat ──────────────────────

    @PostMapping("/send-change-email-otp")
    public ResponseEntity<?> sendChangeEmailOtp(Authentication auth) {
        return sendOtp(auth);
    }
}