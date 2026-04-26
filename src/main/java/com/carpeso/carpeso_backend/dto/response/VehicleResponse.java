package com.carpeso.carpeso_backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VehicleResponse {
    private Long id;
    private String categoryName;
    private String brand;
    private String model;
    private Integer year;
    private BigDecimal price;
    private String color;
    private String fuelType;
    private String transmission;
    private String bodyType;
    private Integer mileage;
    private String description;
    private String engineNumber;
    private String chassisNumber;
    private String plateNumber;
    private Integer warrantyYears;
    private String warrantyDetails;
    private String condition;
    private String status;
    private List<String> imageUrls;
    private String videoUrl;
    private Double averageRating;
    private LocalDateTime createdAt;
    private Integer quantity;
    private List<String> videoUrls;
}