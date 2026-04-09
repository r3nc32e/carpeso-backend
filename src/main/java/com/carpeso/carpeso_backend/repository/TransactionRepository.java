package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);
    List<Transaction> findByVehicleId(Long vehicleId);
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
}