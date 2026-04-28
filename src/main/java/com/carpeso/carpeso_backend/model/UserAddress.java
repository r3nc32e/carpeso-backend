package com.carpeso.carpeso_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_addresses")
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String label; // e.g. "Home", "Office", "Address 1"
    private String cityName;
    private String barangayName;
    private String streetNo;
    private boolean isDefault = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (streetNo != null && !streetNo.isEmpty()) sb.append(streetNo).append(", ");
        if (barangayName != null) sb.append(barangayName).append(", ");
        if (cityName != null) sb.append(cityName);
        return sb.toString();
    }
}