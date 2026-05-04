package com.carpeso.carpeso_backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long id;
    private Long vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private Long buyerId;
    private String buyerFirstName;
    private String buyerLastName;
    private String buyerEmail;
    private int rating;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
    private Long transactionId;  // needed by frontend to match which order was reviewed
}