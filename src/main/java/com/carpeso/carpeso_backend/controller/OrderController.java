package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.model.Order;
import com.carpeso.carpeso_backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/{transactionId}/{userId}/{vehicleId}")
    public ResponseEntity<Order> createOrder(
            @PathVariable Long transactionId,
            @PathVariable Long userId,
            @PathVariable Long vehicleId,
            @RequestBody Order order) {
        return ResponseEntity.ok(
                orderService.createOrder(transactionId, userId, vehicleId, order));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}