package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.dto.response.VehicleResponse;
import com.carpeso.carpeso_backend.model.Category;
import com.carpeso.carpeso_backend.model.Review;
import com.carpeso.carpeso_backend.repository.CategoryRepository;
import com.carpeso.carpeso_backend.service.ReviewService;
import com.carpeso.carpeso_backend.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/vehicles")
    public ResponseEntity<?> getAvailableVehicles() {
        List<VehicleResponse> vehicles =
                vehicleService.getAllVehicles();
        return ResponseEntity.ok(
                ApiResponse.success("Vehicles fetched!", vehicles));
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable Long id) {
        try {
            VehicleResponse vehicle = vehicleService.getVehicleById(id);
            return ResponseEntity.ok(
                    ApiResponse.success("Vehicle fetched!", vehicle));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/vehicles/search")
    public ResponseEntity<?> searchVehicles(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String bodyType,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition) {
        List<VehicleResponse> vehicles = vehicleService.searchVehicles(
                brand, bodyType, fuelType, minPrice, maxPrice, condition);
        return ResponseEntity.ok(
                ApiResponse.success("Search results!", vehicles));
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        List<Category> categories =
                categoryRepository.findByIsActiveTrue();
        return ResponseEntity.ok(
                ApiResponse.success("Categories fetched!", categories));
    }

    @GetMapping("/vehicles/{id}/reviews")
    public ResponseEntity<?> getVehicleReviews(@PathVariable Long id) {
        List<Review> reviews = reviewService.getVehicleReviews(id);
        return ResponseEntity.ok(
                ApiResponse.success("Reviews fetched!", reviews));
    }
}