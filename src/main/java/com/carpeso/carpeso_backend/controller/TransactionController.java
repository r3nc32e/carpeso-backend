package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.model.Transaction;
import com.carpeso.carpeso_backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/{vehicleId}/{userId}")
    public ResponseEntity<Transaction> createTransaction(
            @PathVariable Long vehicleId,
            @PathVariable Long userId,
            @RequestBody Transaction transaction) {
        return ResponseEntity.ok(
                transactionService.createTransaction(vehicleId, userId, transaction));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(
            @PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getUserTransactions(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Transaction> updateStatus(
            @PathVariable Long id,
            @RequestParam Transaction.TransactionStatus status) {
        return ResponseEntity.ok(transactionService.updateStatus(id, status));
    }
}