package com.carpeso.carpeso_backend.model;

import com.carpeso.carpeso_backend.model.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.PENDING;

    private boolean warningIssued = false;

    @ManyToOne
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime moderatedAt;
}