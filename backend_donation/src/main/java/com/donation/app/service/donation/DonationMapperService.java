package com.donation.app.service.donation;

import java.math.BigDecimal;

import com.donation.app.dto.response.DonationResponse;
import com.donation.app.enums.Method;
import com.donation.app.model.Donation;
import com.donation.app.model.Transaction;
import com.donation.app.model.Voucher;
import com.donation.app.repository.TransactionRepository;
import com.donation.app.repository.VoucherRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonationMapperService {

    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;

    public DonationResponse mapToResponse(Donation d) {
        Transaction tx = transactionRepository.findFirstByDonation_IdOrderByTransactionDateDesc(d.getId())
                .orElse(null);
        Voucher v = (tx != null) ? voucherRepository.findFirstByTransaction_Id(tx.getId()).orElse(null) : null;

        String donorName = d.getDonor() != null ? d.getDonor().getName()
                : extractDonorNameFromMessage(d.getMessage());
        String donorEmail = d.getDonor() != null
                ? d.getDonor().getEmail()
                : d.getPayerEmail();

        return mapToResponse(d, tx, v, donorName, donorEmail);
    }

    public DonationResponse mapToResponse(Donation d, Transaction tx, Voucher v, String donorName,
            String donorEmail) {
        return new DonationResponse(
                d.getId(),
                d.getAmount(),
                d.getCurrency(),
                d.getMessage(),
                d.getStatus(),
                d.getDonationDate(),
                d.getDonor() != null ? d.getDonor().getId() : null,
                donorName,
                donorEmail,
                d.getCampaign() != null ? d.getCampaign().getId() : null,
                d.getCampaign() != null ? d.getCampaign().getTitle() : "Campaña General",
                tx != null ? tx.getMethodPay() : Method.CREDIT_CARD,
                tx != null ? tx.getCodeReferential() : ("TX-" + d.getId()),
                v != null ? v.getNumber() : null);
    }

    public Method parseMethod(String raw) {
        if (raw == null)
            return Method.CREDIT_CARD;
        return switch (raw.trim().toLowerCase()) {
            case "card", "credit_card" -> Method.CREDIT_CARD;
            case "debit_card" -> Method.DEBIT_CARD;
            case "transfer", "bank_transfer" -> Method.BANK_TRANSFER;
            case "wallet", "yape" -> Method.YAPE;
            case "plin" -> Method.PLIN;
            case "paypal" -> Method.PAYPAL;
            default -> Method.CREDIT_CARD;
        };
    }

    private String extractDonorNameFromMessage(String msg) {
        if (msg != null && msg.contains(" - ")) {
            String[] parts = msg.split(" - ");
            String candidate = parts[parts.length - 1].trim();
            if (!candidate.isEmpty())
                return candidate;
        }
        return "Donante Solidario";
    }

    private String formatShortName(String fullName) {
        if (fullName == null || fullName.isBlank())
            return "Donante";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0];
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }
}