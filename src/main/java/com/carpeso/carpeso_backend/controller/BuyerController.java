package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.request.ReviewRequest;
import com.carpeso.carpeso_backend.dto.request.TransactionRequest;
import com.carpeso.carpeso_backend.dto.request.WarrantyClaimRequest;
import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.dto.response.TransactionResponse;
import com.carpeso.carpeso_backend.model.Transaction;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.WarrantyClaim;
import com.carpeso.carpeso_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.carpeso.carpeso_backend.model.UserAddress;
import com.carpeso.carpeso_backend.repository.UserAddressRepository;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.service.FileUploadService;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/orders/{id}/receipt")
    public ResponseEntity<?> downloadReceipt(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            Transaction transaction = transactionService.getTransactionEntity(id);

            if (!transaction.getBuyer().getId().equals(buyer.getId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Unauthorized!"));
            }

            if (!transaction.isReceiptGenerated()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Receipt not yet available. Order must be delivered first."));
            }

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
            return ResponseEntity.ok(ApiResponse.success("All marked as read!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User buyer = authService.getCurrentUser(auth.getName());
            transactionService.cancelReservation(id, buyer);
            return ResponseEntity.ok(ApiResponse.success("Reservation cancelled!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            userService.updateProfile(user, request);
            return ResponseEntity.ok(
                    ApiResponse.success("Profile updated!",
                            userService.toResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/profile/password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            userService.changePassword(user,
                    request.get("currentPassword"),
                    request.get("newPassword"));
            return ResponseEntity.ok(ApiResponse.success("Password changed!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Autowired
    private UserAddressRepository userAddressRepository;

    @GetMapping("/addresses")
    public ResponseEntity<?> getAddresses(Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            List<UserAddress> addresses = userAddressRepository
                    .findByUserIdOrderByIsDefaultDescCreatedAtAsc(user.getId());
            return ResponseEntity.ok(ApiResponse.success("Addresses fetched!", addresses));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/addresses")
    public ResponseEntity<?> addAddress(
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            List<UserAddress> existing = userAddressRepository.findByUserId(user.getId());
            if (existing.size() >= 5) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Maximum 5 addresses allowed!"));
            }
            UserAddress address = new UserAddress();
            address.setUser(user);
            address.setLabel(request.getOrDefault("label", "Address " + (existing.size() + 1)));
            address.setCityName(request.get("cityName"));
            address.setBarangayName(request.get("barangayName"));
            address.setStreetNo(request.get("streetNo"));
            address.setDefault(existing.isEmpty());
            userAddressRepository.save(address);
            return ResponseEntity.ok(ApiResponse.success("Address added!", address));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<?> deleteAddress(
            @PathVariable Long id,
            Authentication auth) {
        try {
            userAddressRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Address deleted!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/profile/id/primary")
    public ResponseEntity<?> uploadPrimaryId(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            String url = fileUploadService.uploadImage(file);
            user.setPrimaryIdUrl(url);
            userRepository.save(user);
            return ResponseEntity.ok(
                    ApiResponse.success("Primary ID uploaded!", url));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/profile/id/secondary")
    public ResponseEntity<?> uploadSecondaryId(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication auth) {
        try {
            User user = authService.getCurrentUser(auth.getName());
            String url = fileUploadService.uploadImage(file);
            user.setSecondaryIdUrl(url);
            userRepository.save(user);
            return ResponseEntity.ok(
                    ApiResponse.success("Secondary ID uploaded!", url));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}