package com.carpeso.carpeso_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private BigDecimal price;

    private String color;
    private String fuelType;
    private String transmission;
    private String bodyType;
    private Integer mileage;
    private String description;

    @Enumerated(EnumType.STRING)
    private VehicleStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum VehicleStatus {
        AVAILABLE, RESERVED, SOLD
    }
}