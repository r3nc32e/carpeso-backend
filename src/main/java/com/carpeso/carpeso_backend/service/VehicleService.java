package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.request.VehicleRequest;
import com.carpeso.carpeso_backend.dto.response.VehicleResponse;
import com.carpeso.carpeso_backend.model.Category;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.Vehicle;
import com.carpeso.carpeso_backend.model.enums.VehicleCondition;
import com.carpeso.carpeso_backend.model.enums.VehicleStatus;
import com.carpeso.carpeso_backend.repository.CategoryRepository;
import com.carpeso.carpeso_backend.repository.ReviewRepository;
import com.carpeso.carpeso_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<VehicleResponse> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public VehicleResponse getVehicleById(Long id) {
        return toResponse(vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found!")));
    }

    public List<VehicleResponse> searchVehicles(
            String brand, String bodyType, String fuelType,
            BigDecimal minPrice, BigDecimal maxPrice, String condition) {
        return vehicleRepository.searchVehicles(
                        brand, bodyType, fuelType, minPrice, maxPrice, condition)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public VehicleResponse addVehicle(VehicleRequest request, User addedBy) {
        Vehicle vehicle = new Vehicle();
        setVehicleFields(vehicle, request);
        vehicle.setAddedBy(addedBy);
        vehicleRepository.save(vehicle);
        auditLogService.log("VEHICLE_ADDED", addedBy.getEmail(),
                "Vehicle", String.valueOf(vehicle.getId()),
                "Added: " + vehicle.getBrand() + " " + vehicle.getModel(),
                "system");
        return toResponse(vehicle);
    }

    public VehicleResponse updateVehicle(Long id, VehicleRequest request,
                                         String performedBy) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));
        setVehicleFields(vehicle, request);
        vehicle.setUpdatedAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
        auditLogService.log("VEHICLE_UPDATED", performedBy,
                "Vehicle", String.valueOf(id),
                "Updated: " + vehicle.getBrand() + " " + vehicle.getModel(),
                "system");
        return toResponse(vehicle);
    }

    public void deleteVehicle(Long id, String performedBy) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));
        vehicleRepository.deleteById(id);
        auditLogService.log("VEHICLE_DELETED", performedBy,
                "Vehicle", String.valueOf(id),
                "Deleted: " + vehicle.getBrand() + " " + vehicle.getModel(),
                "system");
    }

    private void setVehicleFields(Vehicle vehicle, VehicleRequest request) {
        if (request.getCategoryId() != null) {
            Category category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found!"));
            vehicle.setCategory(category);
        }
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setPrice(request.getPrice());
        vehicle.setColor(request.getColor());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setTransmission(request.getTransmission());
        vehicle.setBodyType(request.getBodyType());
        vehicle.setMileage(request.getMileage());
        vehicle.setDescription(request.getDescription());
        vehicle.setEngineNumber(request.getEngineNumber());
        vehicle.setChassisNumber(request.getChassisNumber());
        vehicle.setPlateNumber(request.getPlateNumber());
        vehicle.setWarrantyYears(request.getWarrantyYears());
        vehicle.setWarrantyDetails(request.getWarrantyDetails());
        if (request.getCondition() != null) {
            vehicle.setCondition(VehicleCondition.valueOf(
                    request.getCondition().toUpperCase()));
        }
        if (request.getVideoUrl() != null)
            vehicle.setVideoUrl(request.getVideoUrl());
        if (request.getImageUrls() != null) {
            vehicle.getImageUrls().clear();
            vehicle.getImageUrls().addAll(request.getImageUrls());
        }
        if (request.getQuantity() != null)
            vehicle.setQuantity(request.getQuantity());
        if (request.getVideoUrls() != null) {
            vehicle.getVideoUrls().clear();
            vehicle.getVideoUrls().addAll(request.getVideoUrls());
        }
    }

    public VehicleResponse toResponse(Vehicle v) {
        VehicleResponse res = new VehicleResponse();
        res.setId(v.getId());
        if (v.getCategory() != null)
            res.setCategoryName(v.getCategory().getName());
        res.setBrand(v.getBrand());
        res.setModel(v.getModel());
        res.setYear(v.getYear());
        res.setPrice(v.getPrice());
        res.setColor(v.getColor());
        res.setFuelType(v.getFuelType());
        res.setTransmission(v.getTransmission());
        res.setBodyType(v.getBodyType());
        res.setMileage(v.getMileage());
        res.setDescription(v.getDescription());
        res.setEngineNumber(v.getEngineNumber());
        res.setChassisNumber(v.getChassisNumber());
        res.setPlateNumber(v.getPlateNumber());
        res.setWarrantyYears(v.getWarrantyYears());
        res.setWarrantyDetails(v.getWarrantyDetails());
        if (v.getCondition() != null)
            res.setCondition(v.getCondition().name());
        if (v.getStatus() != null)
            res.setStatus(v.getStatus().name());
        res.setImageUrls(v.getImageUrls());
        res.setVideoUrl(v.getVideoUrl());
        res.setCreatedAt(v.getCreatedAt());
        Double avgRating = reviewRepository
                .getAverageRatingByVehicleId(v.getId());
        res.setAverageRating(avgRating);
        res.setQuantity(v.getQuantity());
        res.setVideoUrls(v.getVideoUrls());
        return res;
    }
}