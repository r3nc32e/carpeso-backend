package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.Barangay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BarangayRepository extends JpaRepository<Barangay, Long> {
    List<Barangay> findByCityId(Long cityId);
}