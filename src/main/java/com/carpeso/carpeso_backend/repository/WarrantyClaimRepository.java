package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.WarrantyClaim;
import com.carpeso.carpeso_backend.model.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {
    List<WarrantyClaim> findByBuyerId(Long buyerId);
    List<WarrantyClaim> findByStatus(ClaimStatus status);
    List<WarrantyClaim> findByTransactionId(Long transactionId);
}