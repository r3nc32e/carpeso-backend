package com.carpeso.carpeso_backend.dto.request;

import lombok.Data;

@Data
public class WarrantyClaimRequest {
    private Long transactionId;
    private String issue;
    private String evidenceUrl;
}