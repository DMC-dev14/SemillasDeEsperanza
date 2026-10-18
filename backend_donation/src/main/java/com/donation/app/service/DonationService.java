package com.donation.app.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donation.app.dto.request.DonationRequest;
import com.donation.app.dto.response.DonationResponse;
import com.donation.app.enums.CampaignStatus;
import com.donation.app.enums.Method;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.Campaign;
import com.donation.app.model.Donation;
import com.donation.app.model.Transaction;
import com.donation.app.model.User;
import com.donation.app.model.Voucher;
import com.donation.app.repository.CampaignRepository;
import com.donation.app.repository.DonationRepository;
import com.donation.app.repository.TransactionRepository;
import com.donation.app.repository.UserRepository;
import com.donation.app.repository.VoucherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;

    @Transactional
    public DonationResponse createDonation(DonationRequest request, User authenticatedUser) {
        Campaign campaign = campaignRepository.findById(request.campaignId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "La campaña seleccionada no existe"));

        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Esta campaña no está activa para recibir donaciones");
        }

        String donorName = request.donorName().trim();
        String donorEmail = request.donorEmail().trim().toLowerCase();

        User donor = authenticatedUser;
        if (donor == null) {
            donor = userRepository.findByEmail(donorEmail).orElse(null);
        }

        String cleanMsg = request.message() != null && !request.message().isBlank()
                ? request.message().trim()
                : ("Donación " + (request.donationType() != null ? request.donationType() : "única") + " - " + donorName);
        if (cleanMsg.length() > 250) {
            cleanMsg = cleanMsg.substring(0, 250);
        }

        Donation donation = Donation.builder()
                .amount(request.amount())
                .currency("PEN")
                .message(cleanMsg)
                .status(PaymentStatus.COMPLETED)
                .donor(donor)
                .campaign(campaign)
                .build();
        Donation savedDonation = donationRepository.save(donation);

        Method method = parseMethod(request.paymentMethod());
        String txCode = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
                .codeReferential(txCode)
                .methodPay(method)
                .amount(request.amount())
                .currency("PEN")
                .status(PaymentStatus.COMPLETED)
                .donation(savedDonation)
                .build();
        Transaction savedTx = transactionRepository.save(transaction);

        String voucherNum = String.format("REC-2026-%05d", savedDonation.getId());
        Voucher voucher = new Voucher();
        voucher.setNumber(voucherNum);
        voucher.setEmailSend(donorEmail);
        voucher.setFilePath("/api/donations/" + savedDonation.getId());
        voucher.setTransaction(savedTx);
        voucherRepository.save(voucher);

        BigDecimal currentCollected = campaign.getAmountCollected() != null
                ? campaign.getAmountCollected()
                : BigDecimal.ZERO;
        campaign.setAmountCollected(currentCollected.add(request.amount()));
        campaignRepository.save(campaign);

        return mapToResponse(savedDonation, savedTx, voucher, donorName, donorEmail);
    }

    public List<DonationResponse> getMyDonations(Long userId) {
        return donationRepository.findByDonor_IdOrderByDonationDateDesc(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DonationResponse> getAllDonations() {
        return donationRepository.findAllByOrderByDonationDateDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DonationResponse> getRecentCompletedDonations() {
        return donationRepository.findTop10ByStatusOrderByDonationDateDesc(PaymentStatus.COMPLETED).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public DonationResponse updateStatus(Long donationId, PaymentStatus newStatus) {
        if (newStatus == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Donación no encontrada"));

        PaymentStatus oldStatus = donation.getStatus();
        if (oldStatus != newStatus) {
            Campaign campaign = donation.getCampaign();
            if (campaign != null) {
                BigDecimal current = campaign.getAmountCollected() != null ? campaign.getAmountCollected() : BigDecimal.ZERO;
                if (oldStatus == PaymentStatus.COMPLETED && newStatus != PaymentStatus.COMPLETED) {
                    BigDecimal updated = current.subtract(donation.getAmount());
                    campaign.setAmountCollected(updated.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : updated);
                    campaignRepository.save(campaign);
                } else if (oldStatus != PaymentStatus.COMPLETED && newStatus == PaymentStatus.COMPLETED) {
                    campaign.setAmountCollected(current.add(donation.getAmount()));
                    campaignRepository.save(campaign);
                }
            }
            donation.setStatus(newStatus);
            donationRepository.save(donation);

            transactionRepository.findFirstByDonation_IdOrderByTransactionDateDesc(donationId).ifPresent(tx -> {
                tx.setStatus(newStatus);
                transactionRepository.save(tx);
            });
        }

        return mapToResponse(donation);
    }

    public Map<String, Object> getClientDashboardStats(Long userId) {
        List<Donation> myDonations = donationRepository.findByDonor_IdOrderByDonationDateDesc(userId);
        BigDecimal totalDonado = myDonations.stream()
                .filter(d -> d.getStatus() == PaymentStatus.COMPLETED)
                .map(Donation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedCount = myDonations.stream()
                .filter(d -> d.getStatus() == PaymentStatus.COMPLETED)
                .count();

        long distinctCampaigns = myDonations.stream()
                .filter(d -> d.getStatus() == PaymentStatus.COMPLETED && d.getCampaign() != null)
                .map(d -> d.getCampaign().getId())
                .distinct()
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDonado", totalDonado);
        summary.put("cantidadDonaciones", completedCount);
        summary.put("proyectosApoyados", distinctCampaigns);
        return summary;
    }

    public List<Map<String, Object>> getTopDonors() {
        List<Donation> allCompleted = donationRepository.findAllByOrderByDonationDateDesc().stream()
                .filter(d -> d.getStatus() == PaymentStatus.COMPLETED)
                .toList();

        Map<String, BigDecimal> totalsByDonor = new LinkedHashMap<>();
        Map<String, Integer> countsByDonor = new LinkedHashMap<>();
        Map<String, String> displayNames = new LinkedHashMap<>();

        for (Donation d : allCompleted) {
            String key;
            String name;
            if (d.getDonor() != null) {
                key = "USER_" + d.getDonor().getId();
                name = formatShortName(d.getDonor().getName());
            } else {
                key = "ANON_" + d.getId();
                name = extractDonorNameFromMessage(d.getMessage());
            }
            totalsByDonor.merge(key, d.getAmount(), BigDecimal::add);
            countsByDonor.merge(key, 1, Integer::sum);
            displayNames.putIfAbsent(key, name);
        }

        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(totalsByDonor.entrySet());
        sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        String[] badges = {"🥇", "🥈", "🥉", "#4", "#5"};
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(5, sorted.size());
        for (int i = 0; i < limit; i++) {
            String key = sorted.get(i).getKey();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", i + 1);
            row.put("badge", badges[i]);
            row.put("name", displayNames.getOrDefault(key, "Donante Solidario"));
            row.put("donations", countsByDonor.getOrDefault(key, 1));
            row.put("total", sorted.get(i).getValue());
            result.add(row);
        }
        return result;
    }

    public DonationResponse mapToResponse(Donation d) {
        Transaction tx = transactionRepository.findFirstByDonation_IdOrderByTransactionDateDesc(d.getId()).orElse(null);
        Voucher v = (tx != null) ? voucherRepository.findFirstByTransaction_Id(tx.getId()).orElse(null) : null;

        String donorName = d.getDonor() != null ? d.getDonor().getName() : extractDonorNameFromMessage(d.getMessage());
        String donorEmail = d.getDonor() != null ? d.getDonor().getEmail() : (v != null ? v.getEmailSend() : "donante@semillas.org");

        return mapToResponse(d, tx, v, donorName, donorEmail);
    }

    private DonationResponse mapToResponse(Donation d, Transaction tx, Voucher v, String donorName, String donorEmail) {
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
                v != null ? v.getNumber() : String.format("REC-2026-%05d", d.getId())
        );
    }

    private Method parseMethod(String raw) {
        if (raw == null) return Method.CREDIT_CARD;
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
            if (!candidate.isEmpty()) return candidate;
        }
        return "Donante Solidario";
    }

    private String formatShortName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Donante";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }
}
