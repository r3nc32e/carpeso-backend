package com.carpeso.carpeso_backend.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
    private String phone;
    private String civilStatus;
    private String occupation;
    private String employmentStatus;
    private String cityName;
    private String barangayName;
    private String streetNo;
    private String lotNo;
    private String postalCode;
    private String address;
    private String preferredPaymentMode;
}