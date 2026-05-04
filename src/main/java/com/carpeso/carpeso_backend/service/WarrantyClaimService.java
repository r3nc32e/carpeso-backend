package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.request.WarrantyClaimRequest;
import com.carpeso.carpeso_backend.model.Transaction;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.model.WarrantyClaim;
import com.carpeso.carpeso_backend.model.enums.ClaimStatus;
import com.carpeso.carpeso_backend.model.enums.NotificationType;
import com.carpeso.carpeso_backend.model.enums.Role;
import com.carpeso.carpeso_backend.repository.TransactionRepository;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.repository.WarrantyClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WarrantyClaimService {

    @Autowired
    private WarrantyClaimRepository warrantyClaimRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EmailService emailService;

    public WarrantyClaim createClaim(WarrantyClaimRequest request,
                                     User buyer) {
        Transaction transaction = transactionRepository
                .findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found!"));

        WarrantyClaim claim = new WarrantyClaim();
        claim.setTransaction(transaction);
        claim.setBuyer(buyer);
        claim.setIssue(request.getIssue());
        claim.setEvidenceUrl(request.getEvidenceUrl());
        claim.setStatus(ClaimStatus.OPEN);
        warrantyClaimRepository.save(claim);

        // Notify TRANSACTION_MANAGER admins
        userRepository.findByRole(Role.ADMIN).forEach(admin -> {
            if (admin.getPrivileges() != null &&
                    admin.getPrivileges().contains(
                            com.carpeso.carpeso_backend.model.enums
                                    .AdminPrivilege.TRANSACTION_MANAGER)) {
                notificationService.send(admin,
                        "New Warranty Claim",
                        buyer.getFullName() + " filed a warranty claim.",
                        NotificationType.WARRANTY_CLAIM,
                        "/admin/warranty-claims");
            }
        });

        userRepository.findByRole(Role.SUPERADMIN).forEach(sa ->
                notificationService.send(sa,
                        "New Warranty Claim",
                        buyer.getFullName() + " filed a warranty claim.",
                        NotificationType.WARRANTY_CLAIM,
                        "/admin/warranty-claims"));

        auditLogService.log("WARRANTY_CLAIM_CREATED", buyer.getEmail(),
                "WarrantyClaim", String.valueOf(claim.getId()),
                "Claim filed for transaction #" + transaction.getId(),
                "system");

        return claim;
    }

    // ✅ Admin update via string status + adminResponse + email
    public void updateClaimStatus(Long claimId, String statusStr,
                                  String adminResponse, String performedBy) {
        WarrantyClaim claim = warrantyClaimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found!"));

        // Convert string to enum
        ClaimStatus newStatus;
        try {
            newStatus = ClaimStatus.valueOf(
                    statusStr.toUpperCase().replace(" ", "_"));
        } catch (Exception e) {
            newStatus = ClaimStatus.OPEN;
        }

        claim.setStatus(newStatus);
        claim.setAdminResponse(adminResponse);
        claim.setUpdatedAt(LocalDateTime.now());

        if (newStatus == ClaimStatus.RESOLVED ||
                newStatus == ClaimStatus.CLOSED) {
            claim.setResolvedAt(LocalDateTime.now());
        }

        warrantyClaimRepository.save(claim);

        // Send email to buyer
        try {
            emailService.sendWarrantyClaimUpdate(
                    claim.getBuyer().getEmail(),
                    claim.getBuyer().getFirstName(),
                    statusStr,
                    adminResponse);
        } catch (Exception e) {
            System.out.println("Email failed: " + e.getMessage());
        }

        // Notify buyer
        notificationService.send(claim.getBuyer(),
                "Warranty Claim Update",
                "Your warranty claim has been " + statusStr + ".",
                NotificationType.WARRANTY_CLAIM,
                "/buyer/warranty-claims");

        auditLogService.log("WARRANTY_CLAIM_UPDATED", performedBy,
                "WarrantyClaim", String.valueOf(claimId),
                "Status updated to: " + statusStr, "system");
    }

    public List<WarrantyClaim> getAllClaims() {
        return warrantyClaimRepository.findAll();
    }

    public List<WarrantyClaim> getBuyerClaims(Long buyerId) {
        return warrantyClaimRepository.findByBuyerId(buyerId);
    }
}