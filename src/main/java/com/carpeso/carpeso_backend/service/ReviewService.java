package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.request.ReviewRequest;
import com.carpeso.carpeso_backend.model.Review;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.Vehicle;
import com.carpeso.carpeso_backend.model.enums.NotificationType;
import com.carpeso.carpeso_backend.model.enums.ReviewStatus;
import com.carpeso.carpeso_backend.model.enums.Role;
import com.carpeso.carpeso_backend.repository.ReviewRepository;
import com.carpeso.carpeso_backend.repository.TransactionRepository;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public Review createReview(ReviewRequest request, User buyer) {
        Vehicle vehicle = vehicleRepository
                .findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));

        if (reviewRepository.existsByBuyerIdAndTransactionId(
                buyer.getId(), request.getTransactionId())) {
            throw new RuntimeException(
                    "You already reviewed this transaction!");
        }

        Review review = new Review();
        review.setVehicle(vehicle);
        review.setBuyer(buyer);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setStatus(ReviewStatus.PENDING);

        if (request.getTransactionId() != null) {
            transactionRepository.findById(request.getTransactionId())
                    .ifPresent(review::setTransaction);
        }

        reviewRepository.save(review);

        // Notify moderators
        userRepository.findByRole(Role.ADMIN).forEach(admin -> {
            if (admin.getPrivileges() != null &&
                    admin.getPrivileges().contains(
                            com.carpeso.carpeso_backend.model.enums
                                    .AdminPrivilege.CONTENT_MODERATOR)) {
                notificationService.send(admin,
                        "New Review Pending",
                        buyer.getFullName() + " submitted a review.",
                        NotificationType.REVIEW,
                        "/admin/reviews");
            }
        });

        auditLogService.log("REVIEW_CREATED", buyer.getEmail(),
                "Review", String.valueOf(review.getId()),
                "Review for vehicle #" + vehicle.getId(), "system");

        return review;
    }

    public Review moderateReview(Long reviewId, ReviewStatus status,
                                 User moderator) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found!"));

        review.setStatus(status);
        review.setModeratedBy(moderator);
        review.setModeratedAt(LocalDateTime.now());
        reviewRepository.save(review);

        notificationService.send(review.getBuyer(),
                "Review " + status.name(),
                "Your review has been " + status.name().toLowerCase(),
                NotificationType.REVIEW, null);

        auditLogService.log("REVIEW_MODERATED", moderator.getEmail(),
                "Review", String.valueOf(reviewId),
                "Status: " + status, "system");

        return review;
    }

    public List<Review> getVehicleReviews(Long vehicleId) {
        return reviewRepository.findByVehicleIdAndStatus(
                vehicleId, ReviewStatus.APPROVED);
    }

    public List<Review> getPendingReviews() {
        return reviewRepository.findByStatus(ReviewStatus.PENDING);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }
}