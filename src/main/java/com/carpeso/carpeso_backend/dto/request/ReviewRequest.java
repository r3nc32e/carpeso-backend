package com.carpeso.carpeso_backend.dto.request;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long vehicleId;
    private Long transactionId; // optional
    private Integer rating;
    private String comment;
}