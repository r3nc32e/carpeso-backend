package com.carpeso.carpeso_backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
    private String phone;
    private String civilStatus;
    private String occupation;
    private String employmentStatus;
    private String address;
    private String cityName;
    private String barangayName;
    private String role;
    private Set<String> privileges;
    private boolean isActive;
    private boolean isSuspended;
    private int warningCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String primaryIdUrl;
    private String secondaryIdUrl;
    private String streetNo;
}