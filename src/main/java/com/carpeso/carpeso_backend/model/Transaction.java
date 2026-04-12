package com.carpeso.carpeso_backend.model;

import com.carpeso.carpeso_backend.model.enums.PaymentMode;
import com.carpeso.carpeso_backend.model.enums.TransactionStatus;
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
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private BigDecimal amount;

    private String deliveryAddress;
    private String deliveryNotes;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime actualDelivery;

    private LocalDateTime warrantyStartDate;
    private LocalDateTime warrantyEndDate;

    private String paymentProofUrl;
    private boolean paymentConfirmed = false;
    private String bankDocumentUrl;

    private String idVerificationUrl;
    private boolean idVerified = false;

    @Column(length = 1000)
    private String adminNotes;

    private String receiptNumber;
    private String receiptUrl;
    private boolean receiptGenerated = false;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}