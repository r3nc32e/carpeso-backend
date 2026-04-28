package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.request.VehicleRequest;
import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.enums.AdminPrivilege;
import com.carpeso.carpeso_backend.model.enums.Role;
import com.carpeso.carpeso_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private AuthService authService;
    @Autowired private VehicleService vehicleService;
    @Autowired private CategoryService categoryService; //red line/error
    @Autowired private TransactionService transactionService;
    @Autowired private UserService userService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private ReviewService reviewService;
    @Autowired private ReceiptService receiptService;

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User getAdmin(Authentication auth) {
        return authService.getCurrentUser(auth.getName());
    }

    private boolean isSuperAdmin(User user) {
        return user.getRole() == Role.SUPERADMIN;
    }

    private boolean hasPrivilege(User user, AdminPrivilege privilege) {
        if (isSuperAdmin(user)) return true;
        return user.getPrivileges() != null &&
                user.getPrivileges().contains(privilege);
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(
                        "Access denied! You don't have permission for this action."));
    }

    // ─── VEHICLES — INVENTORY_MANAGER ────────────────────────────────────────

    @GetMapping("/vehicles")
    public ResponseEntity<?> getVehicles(Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.INVENTORY_MANAGER))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Vehicles fetched!",
                            vehicleService.getAllVehicles()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/vehicles")
    public ResponseEntity<?> addVehicle(
            @RequestBody VehicleRequest request,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.INVENTORY_MANAGER))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Vehicle added!",
                            vehicleService.addVehicle(request, admin)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<?> updateVehicle(
            @PathVariable Long id,
            @RequestBody VehicleRequest request,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.INVENTORY_MANAGER))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Vehicle updated!",
                            vehicleService.updateVehicle(id, request,
                                    admin.getEmail())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.INVENTORY_MANAGER))
                return forbidden();
            vehicleService.deleteVehicle(id, admin.getEmail());
            return ResponseEntity.ok(
                    ApiResponse.success("Vehicle deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── CATEGORIES — INVENTORY_MANAGER ──────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.INVENTORY_MANAGER))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Categories fetched!",
                            categoryService.getAllCategories()));//red line/error
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(
            @RequestBody java.util.Map<String, String> request,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.INVENTORY_MANAGER))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Category added!",
                            categoryService.addCategory( //red line/error
                                    request.get("name"), admin.getEmail())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.INVENTORY_MANAGER))
                return forbidden();
            categoryService.deleteCategory(id, admin.getEmail());//red line/error
            return ResponseEntity.ok(
                    ApiResponse.success("Category deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── TRANSACTIONS — TRANSACTION_MANAGER ──────────────────────────────────

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.TRANSACTION_MANAGER))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Transactions fetched!",
                            transactionService.getAllTransactions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/transactions/{id}/status")
    public ResponseEntity<?> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.TRANSACTION_MANAGER))
                return forbidden();
            transactionService.updateStatus(id, status, notes,
                    admin.getEmail());
            return ResponseEntity.ok(
                    ApiResponse.success("Status updated!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── USERS — ACCOUNT_MANAGER ──────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.ACCOUNT_MANAGER))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Users fetched!",
                            userService.getAllBuyers()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/warn")
    public ResponseEntity<?> warnUser(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.ACCOUNT_MANAGER))
                return forbidden();
            userService.warnUser(id, request.get("reason"),
                    admin.getEmail());
            return ResponseEntity.ok(
                    ApiResponse.success("Warning issued!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendUser(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.ACCOUNT_MANAGER))
                return forbidden();
            userService.suspendUser(id, admin.getEmail());
            return ResponseEntity.ok(
                    ApiResponse.success("User suspended!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/unsuspend")
    public ResponseEntity<?> unsuspendUser(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.ACCOUNT_MANAGER))
                return forbidden();
            userService.unsuspendUser(id, admin.getEmail());
            return ResponseEntity.ok(
                    ApiResponse.success("User unsuspended!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.ACCOUNT_MANAGER))
                return forbidden();
            userService.deleteUser(id, admin.getEmail());
            return ResponseEntity.ok(
                    ApiResponse.success("User deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── REVIEWS — CONTENT_MODERATOR ─────────────────────────────────────────

    @GetMapping("/reviews")
    public ResponseEntity<?> getReviews(Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.CONTENT_MODERATOR))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Reviews fetched!",
                            reviewService.getAllReviews()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.CONTENT_MODERATOR))
                return forbidden();
            reviewService.deleteReview(id, admin.getEmail());
            return ResponseEntity.ok(
                    ApiResponse.success("Review deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── AUDIT LOGS — SALES_ANALYST ──────────────────────────────────────────

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.SALES_ANALYST))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Audit logs fetched!",
                            auditLogService.getAllLogs()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── SALES ANALYTICS — SALES_ANALYST ─────────────────────────────────────

    @GetMapping("/sales/stats")
    public ResponseEntity<?> getSalesStats(Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.SALES_ANALYST))
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Stats fetched!",
                            transactionService.getSalesStats()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/sales/report")
    public ResponseEntity<?> generateSalesReport(
            @RequestParam(defaultValue = "month") String period,
            Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (!hasPrivilege(admin, AdminPrivilege.SALES_ANALYST))
                return forbidden();
            byte[] pdf = receiptService.generateSalesReport(
                    transactionService.getAllTransactions(), period);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition",
                            "attachment; filename=sales-report-" + period + ".pdf")
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── OVERVIEW — ALL ADMINS ────────────────────────────────────────────────

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(Authentication auth) {
        try {
            User admin = getAdmin(auth);
            if (admin.getRole() != Role.ADMIN &&
                    admin.getRole() != Role.SUPERADMIN)
                return forbidden();
            return ResponseEntity.ok(
                    ApiResponse.success("Overview fetched!",
                            transactionService.getOverviewStats()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}