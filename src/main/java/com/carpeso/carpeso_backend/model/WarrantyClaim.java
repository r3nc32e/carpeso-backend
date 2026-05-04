package com.carpeso.carpeso_backend.model;

import com.carpeso.carpeso_backend.model.enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "warranty_claims")
public class WarrantyClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Column(nullable = false, length = 2000)
    private String issue;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status = ClaimStatus.OPEN;

    @Column(length = 2000)
    private String adminNotes;

    @Column(length = 2000)
    private String adminResponse;

    private String evidenceUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}