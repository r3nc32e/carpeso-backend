package com.carpeso.carpeso_backend.model;

import com.carpeso.carpeso_backend.model.enums.VehicleCondition;
import com.carpeso.carpeso_backend.model.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

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

    @Column(length = 2000)
    private String description;

    private String engineNumber;
    private String chassisNumber;
    private String plateNumber;

    private Integer warrantyYears;
    private String warrantyDetails;

    @Enumerated(EnumType.STRING)
    private VehicleCondition condition;

    @Enumerated(EnumType.STRING)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @ElementCollection
    @CollectionTable(name = "vehicle_images",
            joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "image_url")
    private List<String> imageUrls;

    @ManyToOne
    @JoinColumn(name = "added_by")
    private User addedBy;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}