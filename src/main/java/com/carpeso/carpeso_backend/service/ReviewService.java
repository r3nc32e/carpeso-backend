package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.request.ReviewRequest;
import com.carpeso.carpeso_backend.dto.response.ReviewResponse;
import com.carpeso.carpeso_backend.model.Review;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.Vehicle;
import com.carpeso.carpeso_backend.model.enums.ReviewStatus;
import com.carpeso.carpeso_backend.repository.ReviewRepository;
import com.carpeso.carpeso_backend.repository.TransactionRepository;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogService auditLogService;

    public Review createReview(ReviewRequest request, User buyer) {
        Vehicle vehicle = vehicleRepository
                .findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));

        Review review = new Review();
        review.setVehicle(vehicle);
        review.setBuyer(buyer);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setStatus(ReviewStatus.APPROVED);

        if (request.getTransactionId() != null) {
            transactionRepository.findById(request.getTransactionId())
                    .ifPresent(review::setTransaction);
        }

        reviewRepository.save(review);

        auditLogService.log("REVIEW_CREATED", buyer.getEmail(),
                "Review", String.valueOf(review.getId()),
                "Review for vehicle #" + vehicle.getId(), "system");

        return review;
    }

    public void deleteReview(Long reviewId, String performedBy) {
        reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found!"));
        reviewRepository.deleteById(reviewId);
        auditLogService.log("REVIEW_DELETED", performedBy,
                "Review", String.valueOf(reviewId),
                "Review deleted", "system");
    }

    public Review moderateReview(Long reviewId, ReviewStatus status, User moderator) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found!"));
        review.setStatus(status);
        review.setModeratedBy(moderator);
        review.setModeratedAt(LocalDateTime.now());
        reviewRepository.save(review);
        return review;
    }

    public List<Review> getVehicleReviews(Long vehicleId) {
        return reviewRepository.findByVehicleIdAndStatus(vehicleId, ReviewStatus.APPROVED);
    }

    // Returns ReviewResponse for the public API — safe, no raw User entity exposed
    public List<ReviewResponse> getVehicleReviewResponses(Long vehicleId) {
        return reviewRepository.findByVehicleIdAndStatus(vehicleId, ReviewStatus.APPROVED)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<Review> getPendingReviews() {
        return reviewRepository.findByStatus(ReviewStatus.PENDING);
    }

    // All reviews — used by admin
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // All reviews by a specific buyer — used on the buyer My Reviews page
    public List<ReviewResponse> getBuyerReviews(Long buyerId) {
        return reviewRepository.findByBuyerId(buyerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReviewResponse toResponse(Review review) {
        ReviewResponse res = new ReviewResponse();
        res.setId(review.getId());
        res.setRating(review.getRating());
        res.setComment(review.getComment());
        if (review.getStatus() != null)
            res.setStatus(review.getStatus().name());
        res.setCreatedAt(review.getCreatedAt());

        if (review.getBuyer() != null) {
            res.setBuyerId(review.getBuyer().getId());
            res.setBuyerFirstName(review.getBuyer().getFirstName());
            res.setBuyerLastName(review.getBuyer().getLastName());
            res.setBuyerEmail(review.getBuyer().getEmail());
        }

        if (review.getVehicle() != null) {
            res.setVehicleId(review.getVehicle().getId());
            res.setVehicleBrand(review.getVehicle().getBrand());
            res.setVehicleModel(review.getVehicle().getModel());
        }

        // transactionId — used by frontend to detect which orders are already reviewed
        if (review.getTransaction() != null) {
            res.setTransactionId(review.getTransaction().getId());
        }

        return res;
    }
}