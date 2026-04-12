package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.Transaction;
import com.carpeso.carpeso_backend.model.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBuyerId(Long buyerId);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByHandledById(Long adminId);

    Optional<Transaction> findByBuyerIdAndStatusIn(
            Long buyerId, List<TransactionStatus> statuses);

    boolean existsByBuyerIdAndStatusIn(
            Long buyerId, List<TransactionStatus> statuses);
}