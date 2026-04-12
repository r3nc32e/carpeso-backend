package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.request.ReviewRequest;
import com.carpeso.carpeso_backend.dto.request.TransactionRequest;
import com.carpeso.carpeso_backend.dto.request.WarrantyClaimRequest;
import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.dto.response.TransactionResponse;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.WarrantyClaim;
import com.carpeso.carpeso_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/buyer")
@CrossOrigin(origins = "*")
public class BuyerController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WarrantyClaimService warrantyClaimService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(
            @RequestBody TransactionRequest request,
            Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            TransactionResponse response =
                    transactionService.createReservation(request, buyer);
            return ResponseEntity.ok(
                    ApiResponse.success("Reservation submitted!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getMyOrders(Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            List<TransactionResponse> orders =
                    transactionService.getBuyerTransactions(buyer.getId());
            return ResponseEntity.ok(
                    ApiResponse.success("Orders fetched!", orders));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/warranty-claims")
    public ResponseEntity<?> fileClaim(
            @RequestBody WarrantyClaimRequest request,
            Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            WarrantyClaim claim =
                    warrantyClaimService.createClaim(request, buyer);
            return ResponseEntity.ok(
                    ApiResponse.success("Claim filed!", claim));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/warranty-claims")
    public ResponseEntity<?> getMyClaims(Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            List<WarrantyClaim> claims =
                    warrantyClaimService.getBuyerClaims(buyer.getId());
            return ResponseEntity.ok(
                    ApiResponse.success("Claims fetched!", claims));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> submitReview(
            @RequestBody ReviewRequest request,
            Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            return ResponseEntity.ok(ApiResponse.success(
                    "Review submitted!",
                    reviewService.createReview(request, buyer)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            return ResponseEntity.ok(ApiResponse.success(
                    "Notifications fetched!",
                    notificationService.getUserNotifications(buyer.getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read!"));
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            notificationService.markAllAsRead(buyer.getId());
            return ResponseEntity.ok(
                    ApiResponse.success("All marked as read!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}