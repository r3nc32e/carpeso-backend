package com.carpeso.carpeso_backend.dto.request;

import lombok.Data;

@Data
public class TransactionRequest {
    private Long vehicleId;
    private String paymentMode;
    private String deliveryAddress;
    private String deliveryNotes;
}