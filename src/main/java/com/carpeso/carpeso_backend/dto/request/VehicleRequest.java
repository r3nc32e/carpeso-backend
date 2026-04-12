package com.carpeso.carpeso_backend.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VehicleRequest {
    private Long categoryId;
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
}