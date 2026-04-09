package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(Vehicle.VehicleStatus status);
    List<Vehicle> findByBrandContainingIgnoreCase(String brand);
    List<Vehicle> findByBodyType(String bodyType);
}