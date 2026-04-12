package com.carpeso.carpeso_backend.model;

import com.carpeso.carpeso_backend.model.enums.AdminPrivilege;
import com.carpeso.carpeso_backend.model.enums.PaymentMode;
import com.carpeso.carpeso_backend.model.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

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
    private String streetNo;
    private String lotNo;
    private String postalCode;

    @Enumerated(EnumType.STRING)
    private PaymentMode preferredPaymentMode;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "admin_privileges",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "privilege")
    private Set<AdminPrivilege> privileges;

    private boolean isActive = true;
    private boolean isSuspended = false;
    private int warningCount = 0;

    private String otpCode;
    private LocalDateTime otpExpiry;
    private boolean otpVerified = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLogin;

    public String getFullName() {
        StringBuilder name = new StringBuilder();
        if (firstName != null) name.append(firstName).append(" ");
        if (middleName != null && !middleName.isEmpty())
            name.append(middleName).append(" ");
        if (lastName != null) name.append(lastName);
        if (suffix != null && !suffix.isEmpty())
            name.append(", ").append(suffix);
        return name.toString().trim();
    }
}