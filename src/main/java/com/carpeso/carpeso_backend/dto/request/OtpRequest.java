package com.carpeso.carpeso_backend.dto.request;

import lombok.Data;

@Data
public class OtpRequest {
    private String email;
    private String otp;
}