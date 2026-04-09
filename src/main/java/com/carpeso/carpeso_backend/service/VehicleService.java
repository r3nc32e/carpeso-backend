package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.model.Vehicle;
import com.carpeso.carpeso_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus(Vehicle.VehicleStatus.AVAILABLE);
    }

    public Vehicle addVehicle(Vehicle vehicle) {
        vehicle.setStatus(Vehicle.VehicleStatus.AVAILABLE);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Long id, Vehicle updated) {
        Vehicle vehicle = getVehicleById(id);
        vehicle.setBrand(updated.getBrand());
        vehicle.setModel(updated.getModel());
        vehicle.setYear(updated.getYear());
        vehicle.setPrice(updated.getPrice());
        vehicle.setColor(updated.getColor());
        vehicle.setFuelType(updated.getFuelType());
        vehicle.setTransmission(updated.getTransmission());
        vehicle.setBodyType(updated.getBodyType());
        vehicle.setMileage(updated.getMileage());
        vehicle.setDescription(updated.getDescription());
        vehicle.setStatus(updated.getStatus());
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }
}