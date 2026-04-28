package com.carpeso.carpeso_backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private Long id;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleColor;
    private BigDecimal amount;
    private String status;
    private String paymentMode;
    private String deliveryAddress;
    private String adminNotes;
    private String receiptNumber;
    private String receiptUrl;
    private boolean receiptGenerated;
    private LocalDateTime warrantyStartDate;
    private LocalDateTime warrantyEndDate;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String buyerFullName;
    private String buyerEmail;
    private String buyerPhone;
    private Long vehicleId;
    private String buyerCityName;
    private String buyerBarangayName;
    private String buyerStreetNo;
}