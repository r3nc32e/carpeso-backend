package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.Vehicle;
import com.carpeso.carpeso_backend.model.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(VehicleStatus status);
    //List<Vehicle> findByStatusAndIsActiveTrue(VehicleStatus status);

    @Query("SELECT v FROM Vehicle v WHERE " +
            "(:brand IS NULL OR LOWER(v.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) AND " +
            "(:bodyType IS NULL OR v.bodyType = :bodyType) AND " +
            "(:fuelType IS NULL OR v.fuelType = :fuelType) AND " +
            "(:minPrice IS NULL OR v.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR v.price <= :maxPrice) AND " +
            "(:condition IS NULL OR v.condition = :condition) AND " +
            "v.status = 'AVAILABLE'")
    List<Vehicle> searchVehicles(
            @Param("brand") String brand,
            @Param("bodyType") String bodyType,
            @Param("fuelType") String fuelType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("condition") String condition
    );

    boolean existsByIdAndStatus(Long id, VehicleStatus status);
}