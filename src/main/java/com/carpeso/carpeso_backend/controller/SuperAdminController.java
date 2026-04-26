package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.model.enums.AdminPrivilege;
import com.carpeso.carpeso_backend.service.AuditLogService;
import com.carpeso.carpeso_backend.service.EmailService;
import com.carpeso.carpeso_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
@CrossOrigin(origins = "*")
public class SuperAdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/admins")
    public ResponseEntity<?> getAllAdmins() {
        return ResponseEntity.ok(ApiResponse.success(
                "Admins fetched!", userService.getAllAdmins()));
    }

    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String email = (String) request.get("email");
            String password = (String) request.get("password");
            String firstName = (String) request.get("firstName");
            String lastName = (String) request.get("lastName");

            List<String> privilegeList = (List<String>) request.get("privileges");
            Set<AdminPrivilege> privileges = privilegeList == null
                    ? new HashSet<>()
                    : privilegeList.stream()
                      .map(AdminPrivilege::valueOf)
                      .collect(Collectors.toSet());

            userService.createAdmin(email, password, firstName,
                    lastName, privileges, auth.getName(), passwordEncoder);

            // Send admin credentials via email
            try {
                emailService.sendAdminCreated(email, firstName, password, "ADMIN");
            } catch (Exception e) {
                System.out.println("Email notification failed: " + e.getMessage());
            }

            return ResponseEntity.ok(ApiResponse.success("Admin created!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/admins/{id}/privileges")
    public ResponseEntity<?> updatePrivileges(
            @PathVariable Long id,
            @RequestBody Set<String> privileges,
            Authentication auth) {
        try {
            Set<AdminPrivilege> adminPrivileges = privileges.stream()
                    .map(AdminPrivilege::valueOf)
                    .collect(Collectors.toSet());
            userService.updateAdminPrivileges(id, adminPrivileges,
                    auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Privileges updated!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deleteAdmin(
            @PathVariable Long id,
            Authentication auth) {
        try {
            userService.deleteUser(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Admin deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAllLogs() {
        return ResponseEntity.ok(ApiResponse.success(
                "Logs fetched!", auditLogService.getAllLogs()));
    }
}