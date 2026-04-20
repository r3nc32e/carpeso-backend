package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.request.VehicleRequest;
import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.model.Category;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.WarrantyClaim;
import com.carpeso.carpeso_backend.model.enums.ClaimStatus;
import com.carpeso.carpeso_backend.model.enums.ReviewStatus;
import com.carpeso.carpeso_backend.model.enums.TransactionStatus;
import com.carpeso.carpeso_backend.repository.CategoryRepository;
import com.carpeso.carpeso_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.carpeso.carpeso_backend.model.Transaction;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AuthService authService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private WarrantyClaimService warrantyClaimService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReceiptService receiptService;


    // ===== RECEIPT =====
    @GetMapping("/transactions/{id}/receipt")
    public ResponseEntity<?> generateReceipt(
            @PathVariable Long id,
            Authentication auth) {
        try {
            Transaction transaction = transactionService.getTransactionEntity(id);
            byte[] pdf = receiptService.generateReceipt(transaction);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition",
                            "attachment; filename=receipt-" + id + ".pdf")
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== VEHICLES =====
    @GetMapping("/vehicles")
    public ResponseEntity<?> getAllVehicles() {
        return ResponseEntity.ok(ApiResponse.success(
                "Vehicles fetched!", vehicleService.getAllVehicles()));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<?> addVehicle(
            @RequestBody VehicleRequest request,
            Authentication auth) {
        try {
            User admin = authService.getCurrentUser(auth.getName());
            return ResponseEntity.ok(ApiResponse.success(
                    "Vehicle added!",
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
            return ResponseEntity.ok(ApiResponse.success(
                    "Vehicle updated!",
                    vehicleService.updateVehicle(id, request,
                            auth.getName())));
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
            vehicleService.deleteVehicle(id, auth.getName());
            return ResponseEntity.ok(
                    ApiResponse.success("Vehicle deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== CATEGORIES =====
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Categories fetched!", categoryRepository.findAll()));
    }

    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(
            @RequestBody Category category,
            Authentication auth) {
        try {
            categoryRepository.save(category);
            auditLogService.log("CATEGORY_ADDED", auth.getName(),
                    "Category", null,
                    "Added: " + category.getName(), "system");
            return ResponseEntity.ok(
                    ApiResponse.success("Category added!", category));
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
            categoryRepository.deleteById(id);
            auditLogService.log("CATEGORY_DELETED", auth.getName(),
                    "Category", String.valueOf(id),
                    "Category deleted", "system");
            return ResponseEntity.ok(
                    ApiResponse.success("Category deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== TRANSACTIONS =====
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() {
        return ResponseEntity.ok(ApiResponse.success(
                "Transactions fetched!",
                transactionService.getAllTransactions()));
    }

    @PutMapping("/transactions/{id}/status")
    public ResponseEntity<?> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam TransactionStatus status,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Status updated!",
                    transactionService.updateStatus(
                            id, status, notes, auth.getName())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== USERS =====
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(
                "Users fetched!", userService.getAllBuyers()));
    }

    @PostMapping("/users/{id}/warn")
    public ResponseEntity<?> warnUser(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication auth) {
        try {
            userService.warnUser(id, reason, auth.getName());
            return ResponseEntity.ok(
                    ApiResponse.success("Warning issued!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendUser(
            @PathVariable Long id,
            Authentication auth) {
        try {
            userService.suspendUser(id, auth.getName());
            return ResponseEntity.ok(
                    ApiResponse.success("User suspended!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/unsuspend")
    public ResponseEntity<?> unsuspendUser(
            @PathVariable Long id,
            Authentication auth) {
        try {
            userService.unsuspendUser(id, auth.getName());
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
            userService.deleteUser(id, auth.getName());
            return ResponseEntity.ok(
                    ApiResponse.success("User deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== REVIEWS =====
    @GetMapping("/reviews")
    public ResponseEntity<?> getPendingReviews() {
        return ResponseEntity.ok(ApiResponse.success(
                "Reviews fetched!", reviewService.getPendingReviews()));
    }

    @PutMapping("/reviews/{id}/moderate")
    public ResponseEntity<?> moderateReview(
            @PathVariable Long id,
            @RequestParam ReviewStatus status,
            Authentication auth) {
        try {
            User admin = authService.getCurrentUser(auth.getName());
            return ResponseEntity.ok(ApiResponse.success(
                    "Review moderated!",
                    reviewService.moderateReview(id, status, admin)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== WARRANTY CLAIMS =====
    @GetMapping("/warranty-claims")
    public ResponseEntity<?> getAllClaims() {
        return ResponseEntity.ok(ApiResponse.success(
                "Claims fetched!", warrantyClaimService.getAllClaims()));
    }

    @PutMapping("/warranty-claims/{id}")
    public ResponseEntity<?> updateClaim(
            @PathVariable Long id,
            @RequestParam ClaimStatus status,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        try {
            User admin = authService.getCurrentUser(auth.getName());
            WarrantyClaim claim = warrantyClaimService
                    .updateClaimStatus(id, status, notes, admin);
            return ResponseEntity.ok(
                    ApiResponse.success("Claim updated!", claim));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== AUDIT LOGS =====
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs() {
        return ResponseEntity.ok(ApiResponse.success(
                "Logs fetched!", auditLogService.getAllLogs()));
    }

    // ===== NOTIFICATIONS =====
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(Authentication auth) {
        try {
            User admin = authService.getCurrentUser(auth.getName());
            return ResponseEntity.ok(ApiResponse.success(
                    "Notifications fetched!",
                    notificationService.getUserNotifications(admin.getId())));
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
            reviewService.deleteReview(id, auth.getName());
            return ResponseEntity.ok(
                    ApiResponse.success("Review deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}