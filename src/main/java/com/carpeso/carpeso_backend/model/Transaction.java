package com.carpeso.carpeso_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionStatus {
        PENDING, APPROVED, REJECTED, COMPLETED
    }

    public enum TransactionType {
        RESERVATION, DOWNPAYMENT, FULL_PAYMENT
    }
}