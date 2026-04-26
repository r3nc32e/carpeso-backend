package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.request.TransactionRequest;
import com.carpeso.carpeso_backend.dto.response.TransactionResponse;
import com.carpeso.carpeso_backend.model.Transaction;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.Vehicle;
import com.carpeso.carpeso_backend.model.enums.*;
import com.carpeso.carpeso_backend.repository.TransactionRepository;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    private static final List<TransactionStatus> ACTIVE_STATUSES = Arrays.asList(
            TransactionStatus.PENDING,
            TransactionStatus.CONFIRMED,
            TransactionStatus.PREPARING,
            TransactionStatus.READY,
            TransactionStatus.OUT_FOR_DELIVERY
    );

    public TransactionResponse createReservation(
            TransactionRequest request, User buyer) {
        if (transactionRepository.existsByBuyerIdAndStatusIn(
                buyer.getId(), ACTIVE_STATUSES)) {
            throw new RuntimeException(
                    "You already have an active reservation! " +
                            "Please complete or cancel it first.");
        }

        Vehicle vehicle = vehicleRepository
                .findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new RuntimeException("Vehicle is not available!");
        }

        // Decrease quantity
        int qty = vehicle.getQuantity() != null ? vehicle.getQuantity() : 1;
        if (qty <= 0) throw new RuntimeException("No more units available!");
        vehicle.setQuantity(qty - 1);

// Only reserve if quantity is 0
        if (qty - 1 == 0) {
            vehicle.setStatus(VehicleStatus.RESERVED);
        }
        vehicleRepository.save(vehicle);

        Transaction transaction = new Transaction();
        transaction.setVehicle(vehicle);
        transaction.setBuyer(buyer);
        transaction.setAmount(vehicle.getPrice());
        transaction.setDeliveryAddress(request.getDeliveryAddress());
        transaction.setDeliveryNotes(request.getDeliveryNotes());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setExpiresAt(LocalDateTime.now().plusHours(48));

        if (request.getPaymentMode() != null) {
            try {
                transaction.setPaymentMode(PaymentMode.valueOf(
                        request.getPaymentMode().toUpperCase()
                                .replace(" ", "_")));
            } catch (Exception ignored) {}
        }

        vehicle.setStatus(VehicleStatus.RESERVED);
        vehicleRepository.save(vehicle);
        transactionRepository.save(transaction);

        // Notify admins
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        admins.forEach(admin -> {
            if (admin.getPrivileges() != null &&
                    admin.getPrivileges().contains(
                            AdminPrivilege.TRANSACTION_MANAGER)) {
                notificationService.send(admin,
                        "New Reservation",
                        buyer.getFullName() + " reserved " +
                                vehicle.getBrand() + " " + vehicle.getModel(),
                        NotificationType.RESERVATION,
                        "/admin/transactions");
            }
        });

        userRepository.findByRole(Role.SUPERADMIN).forEach(sa ->
                notificationService.send(sa,
                        "New Reservation",
                        buyer.getFullName() + " reserved " +
                                vehicle.getBrand() + " " + vehicle.getModel(),
                        NotificationType.RESERVATION,
                        "/admin/transactions"));

        notificationService.send(buyer,
                "Reservation Submitted",
                "Your reservation for " + vehicle.getBrand() +
                        " " + vehicle.getModel() +
                        " is pending admin approval.",
                NotificationType.RESERVATION,
                "/buyer/orders");

        // Send reservation confirmation email
        try {
            String vehicleName = vehicle.getBrand() + " " + vehicle.getModel();
            String expiresAt = transaction.getExpiresAt()
                    .format(java.time.format.DateTimeFormatter
                            .ofPattern("MMMM dd, yyyy hh:mm a"));
            emailService.sendReservationConfirmation(
                    buyer.getEmail(),
                    buyer.getFirstName(),
                    vehicleName,
                    expiresAt
            );
        } catch (Exception e) {
            System.out.println("Email notification failed: " + e.getMessage());
        }

        auditLogService.log("RESERVATION_CREATED", buyer.getEmail(),
                "Transaction", String.valueOf(transaction.getId()),
                "Reserved: " + vehicle.getBrand() + " " + vehicle.getModel(),
                "system");

        return toResponse(transaction);
    }

    public TransactionResponse updateStatus(Long transactionId,
                                            TransactionStatus newStatus,
                                            String adminNotes,
                                            String performedBy) {
        Transaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found!"));

        TransactionStatus oldStatus = transaction.getStatus();
        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(LocalDateTime.now());

        if (adminNotes != null && !adminNotes.isEmpty()) {
            transaction.setAdminNotes(adminNotes);
        }

        if (newStatus == TransactionStatus.DELIVERED) {
            transaction.setActualDelivery(LocalDateTime.now());
            transaction.setWarrantyStartDate(LocalDateTime.now());
            int warrantyYears = transaction.getVehicle()
                    .getWarrantyYears() != null ?
                    transaction.getVehicle().getWarrantyYears() : 1;
            transaction.setWarrantyEndDate(
                    LocalDateTime.now().plusYears(warrantyYears));

            String receiptNumber = "RCP-" + String.format("%06d", transaction.getId())
                    + "-" + LocalDateTime.now().getYear();
            transaction.setReceiptNumber(receiptNumber);
            transaction.setReceiptGenerated(true);
        }

        if (newStatus == TransactionStatus.COMPLETED) {
            Vehicle vehicle = transaction.getVehicle();
            vehicle.setStatus(VehicleStatus.SOLD);
            vehicleRepository.save(vehicle);
        }

        if (newStatus == TransactionStatus.CANCELLED ||
                newStatus == TransactionStatus.EXPIRED) {
            Vehicle vehicle = transaction.getVehicle();
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        transactionRepository.save(transaction);

        // Send email notification to buyer
        try {
            String vehicleName = transaction.getVehicle().getBrand()
                    + " " + transaction.getVehicle().getModel();
            emailService.sendOrderStatusUpdate(
                    transaction.getBuyer().getEmail(),
                    transaction.getBuyer().getFirstName(),
                    vehicleName,
                    newStatus.name(),
                    adminNotes
            );
        } catch (Exception e) {
            System.out.println("Email notification failed: " + e.getMessage());
        }

        notificationService.send(transaction.getBuyer(),
                "Order Update",
                "Your order status has been updated to: " +
                        newStatus.name().replace("_", " "),
                NotificationType.ORDER_UPDATE,
                "/buyer/orders");

        auditLogService.log("TRANSACTION_STATUS_UPDATED", performedBy,
                "Transaction", String.valueOf(transactionId),
                oldStatus + " → " + newStatus, "system");

        return toResponse(transaction);
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getBuyerTransactions(Long buyerId) {
        return transactionRepository.findByBuyerId(buyerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long id) {
        return toResponse(transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found!")));
    }

    public Transaction getTransactionEntity(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found!"));
    }

    public void cancelReservation(Long transactionId, User buyer) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found!"));

        if (!transaction.getBuyer().getId().equals(buyer.getId())) {
            throw new RuntimeException("Unauthorized!");
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Only PENDING reservations can be cancelled!");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);

        Vehicle vehicle = transaction.getVehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);

        auditLogService.log("RESERVATION_CANCELLED", buyer.getEmail(),
                "Transaction", String.valueOf(transactionId),
                "Buyer cancelled reservation", "system");
    }

    public TransactionResponse toResponse(Transaction t) {
        TransactionResponse res = new TransactionResponse();
        res.setId(t.getId());
        if (t.getVehicle() != null) {
            res.setVehicleBrand(t.getVehicle().getBrand());
            res.setVehicleModel(t.getVehicle().getModel());
            res.setVehicleYear(t.getVehicle().getYear());
            res.setVehicleColor(t.getVehicle().getColor());
            res.setVehicleId(t.getVehicle().getId());
        }
        res.setAmount(t.getAmount());
        if (t.getStatus() != null)
            res.setStatus(t.getStatus().name());
        if (t.getPaymentMode() != null)
            res.setPaymentMode(t.getPaymentMode().name());
        res.setDeliveryAddress(t.getDeliveryAddress());
        res.setAdminNotes(t.getAdminNotes());
        res.setReceiptNumber(t.getReceiptNumber());
        res.setReceiptGenerated(t.isReceiptGenerated());
        res.setWarrantyStartDate(t.getWarrantyStartDate());
        res.setWarrantyEndDate(t.getWarrantyEndDate());
        res.setExpiresAt(t.getExpiresAt());
        res.setCreatedAt(t.getCreatedAt());
        res.setUpdatedAt(t.getUpdatedAt());
        if (t.getBuyer() != null) {
            res.setBuyerFullName(t.getBuyer().getFullName());
            res.setBuyerEmail(t.getBuyer().getEmail());
            res.setBuyerPhone(t.getBuyer().getPhone());
        }
        return res;
    }
}