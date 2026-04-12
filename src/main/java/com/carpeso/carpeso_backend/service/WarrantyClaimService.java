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

    public WarrantyClaim createClaim(WarrantyClaimRequest request,
                                     User buyer) {
        Transaction transaction = transactionRepository
                .findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found!"));

        WarrantyClaim claim = new WarrantyClaim();
        claim.setTransaction(transaction);
        claim.setBuyer(buyer);
        claim.setIssue(request.getIssue());
        claim.setEvidenceUrl(request.getEvidenceUrl());
        claim.setStatus(ClaimStatus.OPEN);
        warrantyClaimRepository.save(claim);

        // Notify admins
        userRepository.findByRole(Role.ADMIN).forEach(admin ->
                notificationService.send(admin,
                        "New Warranty Claim",
                        buyer.getFullName() + " filed a warranty claim.",
                        NotificationType.WARRANTY_CLAIM,
                        "/admin/warranty-claims"));

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

    public WarrantyClaim updateClaimStatus(Long claimId,
                                           ClaimStatus status,
                                           String adminNotes,
                                           User admin) {
        WarrantyClaim claim = warrantyClaimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found!"));

        claim.setStatus(status);
        claim.setAdminNotes(adminNotes);
        claim.setHandledBy(admin);
        claim.setUpdatedAt(LocalDateTime.now());

        if (status == ClaimStatus.RESOLVED) {
            claim.setResolvedAt(LocalDateTime.now());
        }

        warrantyClaimRepository.save(claim);

        notificationService.send(claim.getBuyer(),
                "Warranty Claim Update",
                "Your warranty claim status: " + status.name(),
                NotificationType.WARRANTY_CLAIM,
                "/buyer/claims");

        auditLogService.log("WARRANTY_CLAIM_UPDATED", admin.getEmail(),
                "WarrantyClaim", String.valueOf(claimId),
                "Status updated to: " + status, "system");

        return claim;
    }

    public List<WarrantyClaim> getAllClaims() {
        return warrantyClaimRepository.findAll();
    }

    public List<WarrantyClaim> getBuyerClaims(Long buyerId) {
        return warrantyClaimRepository.findByBuyerId(buyerId);
    }
}