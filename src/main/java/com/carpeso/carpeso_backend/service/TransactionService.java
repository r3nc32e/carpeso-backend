package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.model.Transaction;
import com.carpeso.carpeso_backend.model.Vehicle;
import com.carpeso.carpeso_backend.repository.TransactionRepository;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    public Transaction createTransaction(Long vehicleId, Long userId,
                                         Transaction transaction) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));
        transaction.setVehicle(vehicle);
        transaction.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!")));
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        vehicle.setStatus(Vehicle.VehicleStatus.RESERVED);
        vehicleRepository.save(vehicle);
        Transaction saved = transactionRepository.save(transaction);
        auditLogService.log(
                "RESERVATION_CREATED",
                transaction.getUser().getUsername(),
                "Transaction",
                String.valueOf(saved.getId()),
                "Reserved: " + vehicle.getBrand() + " " + vehicle.getModel(),
                "system"
        );
        return saved;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getUserTransactions(Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    public Transaction updateStatus(Long id,
                                    Transaction.TransactionStatus status) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found!"));
        transaction.setStatus(status);
        return transactionRepository.save(transaction);
    }
}