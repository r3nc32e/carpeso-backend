package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.model.Order;
import com.carpeso.carpeso_backend.repository.OrderRepository;
import com.carpeso.carpeso_backend.repository.TransactionRepository;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    public Order createOrder(Long transactionId, Long userId,
                             Long vehicleId, Order order) {
        order.setTransaction(transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found!")));
        order.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!")));
        order.setVehicle(vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found!")));
        order.setStatus(Order.OrderStatus.PROCESSING);
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found!"));
        order.setStatus(status);
        return orderRepository.save(order);
    }
}