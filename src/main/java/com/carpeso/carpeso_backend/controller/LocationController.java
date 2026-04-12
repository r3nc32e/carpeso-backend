package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.model.Barangay;
import com.carpeso.carpeso_backend.model.City;
import com.carpeso.carpeso_backend.repository.BarangayRepository;
import com.carpeso.carpeso_backend.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@CrossOrigin(origins = "*")
public class LocationController {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private BarangayRepository barangayRepository;

    @GetMapping("/cities")
    public ResponseEntity<?> getCities() {
        List<City> cities = cityRepository.findAll();
        return ResponseEntity.ok(
                ApiResponse.success("Cities fetched!", cities));
    }

    @GetMapping("/barangays/{cityId}")
    public ResponseEntity<?> getBarangays(@PathVariable Long cityId) {
        List<Barangay> barangays =
                barangayRepository.findByCityId(cityId);
        return ResponseEntity.ok(
                ApiResponse.success("Barangays fetched!", barangays));
    }
}