package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.Review;
import com.carpeso.carpeso_backend.model.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByVehicleId(Long vehicleId);
    List<Review> findByBuyerId(Long buyerId);
    List<Review> findByStatus(ReviewStatus status);
    List<Review> findByVehicleIdAndStatus(Long vehicleId, ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.vehicle.id = :vehicleId AND r.status = 'APPROVED'")
    Double getAverageRatingByVehicleId(@Param("vehicleId") Long vehicleId);

    boolean existsByBuyerIdAndTransactionId(Long buyerId, Long transactionId);
}