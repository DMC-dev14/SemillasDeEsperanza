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
import com.donation.app.dto.response.DonationCreatedResponse;
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
        public DonationCreatedResponse createDonation(
                        DonationRequest request,
                        User authenticatedUser) {

                Campaign campaign = campaignRepository
                                .findById(request.campaignId())
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "La campaña seleccionada no existe"));

                if (campaign.getStatus() != CampaignStatus.ACTIVE) {
                        throw new BusinessException(
                                        HttpStatus.BAD_REQUEST,
                                        "Esta campaña no está activa para recibir donaciones");
                }

                if (request.amount() == null ||
                                request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessException(
                                        HttpStatus.BAD_REQUEST,
                                        "El monto debe ser mayor que cero");
                }

                Method method = parseMethod(request.paymentMethod());

                if (method == Method.YAPE) {
                        if (request.amount().compareTo(new BigDecimal("1.24")) < 0) {
                                throw new BusinessException(HttpStatus.BAD_REQUEST,
                                                "El monto mínimo para Yape es S/ 1.24");
                        }
                        if (request.amount().compareTo(new BigDecimal("2000.00")) > 0) {
                                throw new BusinessException(HttpStatus.BAD_REQUEST,
                                                "El monto máximo para Yape es S/ 2,000.00");
                        }
                }

                String donorName = request.donorName().trim();
                String donorEmail = request.donorEmail()
                                .trim()
                                .toLowerCase();

                String cleanMsg = request.message() != null
                                && !request.message().isBlank()
                                                ? request.message().trim()
                                                : "Donación de " + donorName;

                if (cleanMsg.length() > 250) {
                        cleanMsg = cleanMsg.substring(0, 250);
                }

                Donation donation = Donation.builder()
                                .amount(request.amount())
                                .currency("PEN")
                                .message(cleanMsg)
                                .status(PaymentStatus.PENDING)
                                .payerEmail(donorEmail)
                                .checkoutToken(UUID.randomUUID().toString())
                                .paymentAttemptKey(UUID.randomUUID().toString())
                                .donor(authenticatedUser)
                                .campaign(campaign)
                                .build();

                Donation savedDonation = donationRepository.save(donation);

                Transaction transaction = Transaction.builder()
                                .codeReferential(
                                                "TX-" + UUID.randomUUID()
                                                                .toString()
                                                                .substring(0, 8)
                                                                .toUpperCase())
                                .methodPay(parseMethod(request.paymentMethod()))
                                .amount(request.amount())
                                .currency("PEN")
                                .status(PaymentStatus.PENDING)
                                .donation(savedDonation)
                                .build();

                Transaction savedTx = transactionRepository.save(transaction);

                return new DonationCreatedResponse(
                                mapToResponse(
                                                savedDonation,
                                                savedTx,
                                                null,
                                                donorName,
                                                donorEmail),
                                savedDonation.getCheckoutToken());
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
        public DonationResponse updateStatus(
                        Long donationId,
                        PaymentStatus newStatus) {

                if (newStatus == null) {
                        throw new BusinessException(
                                        HttpStatus.BAD_REQUEST,
                                        "El estado es obligatorio");
                }

                Donation donation = donationRepository
                                .findLockedById(donationId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Donación no encontrada"));

                Transaction transaction = transactionRepository
                                .findFirstByDonation_IdOrderByTransactionDateDesc(donationId)
                                .orElse(null);

                // Impedir cambios manuales en pagos gestionados por pasarela
                if (transaction != null &&
                                (transaction.getMethodPay() == Method.YAPE
                                                || transaction.getMethodPay() == Method.PAYPAL)) {
                        throw new BusinessException(
                                        HttpStatus.FORBIDDEN,
                                        "El estado de pagos " + transaction.getMethodPay().name()
                                                        + " debe verificarse con la pasarela");
                }

                PaymentStatus oldStatus = donation.getStatus();

                if (oldStatus != newStatus) {

                        Campaign campaign = donation.getCampaign();

                        if (campaign != null) {
                                campaign = campaignRepository
                                                .findLockedById(campaign.getId())
                                                .orElseThrow(() -> new BusinessException(
                                                                HttpStatus.NOT_FOUND,
                                                                "Campaña no encontrada"));

                                BigDecimal current = campaign.getAmountCollected() != null
                                                ? campaign.getAmountCollected()
                                                : BigDecimal.ZERO;

                                if (oldStatus == PaymentStatus.COMPLETED &&
                                                newStatus != PaymentStatus.COMPLETED) {

                                        campaign.setAmountCollected(
                                                        current.subtract(donation.getAmount())
                                                                        .max(BigDecimal.ZERO));

                                } else if (oldStatus != PaymentStatus.COMPLETED &&
                                                newStatus == PaymentStatus.COMPLETED) {

                                        campaign.setAmountCollected(
                                                        current.add(donation.getAmount()));
                                }

                                campaignRepository.save(campaign);
                        }

                        donation.setStatus(newStatus);
                        donationRepository.save(donation);

                        if (transaction != null) {
                                transaction.setStatus(newStatus);
                                transactionRepository.save(transaction);
                        }
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

                String[] badges = { "🥇", "🥈", "🥉", "#4", "#5" };
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

        private DonationResponse mapToResponse(Donation d, Transaction tx, Voucher v, String donorName,
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

        private Method parseMethod(String raw) {
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

        @Transactional
        public void confirmYapePayment(
                        Long donationId,
                        String paymentId,
                        BigDecimal paidAmount,
                        String currency) {

                Donation donation = donationRepository
                                .findLockedById(donationId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Donación no encontrada"));

                if (donation.getStatus() == PaymentStatus.COMPLETED) {
                        if (paymentId.equals(donation.getMercadoPagoPaymentId())) {
                                return;
                        }
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La donación ya tiene otro pago confirmado");
                }

                if (donation.getStatus() != PaymentStatus.PENDING) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La donación no está pendiente");
                }

                if (!"PEN".equals(currency) ||
                                donation.getAmount().compareTo(paidAmount) != 0) {
                        throw new BusinessException(
                                        HttpStatus.BAD_REQUEST,
                                        "Los datos del pago no coinciden con la donación");
                }

                Transaction transaction = transactionRepository
                                .findFirstByDonation_IdOrderByTransactionDateDesc(
                                                donationId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Transacción no encontrada"));

                if (transaction.getMethodPay() != Method.YAPE) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La transacción no corresponde a Yape");
                }

                Campaign campaign = campaignRepository
                                .findLockedById(donation.getCampaign().getId())
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Campaña no encontrada"));

                donation.setMercadoPagoPaymentId(paymentId);
                donation.setStatus(PaymentStatus.COMPLETED);

                transaction.setStatus(PaymentStatus.COMPLETED);

                BigDecimal collected = campaign.getAmountCollected() != null
                                ? campaign.getAmountCollected()
                                : BigDecimal.ZERO;

                campaign.setAmountCollected(
                                collected.add(donation.getAmount()));

                Voucher voucher = new Voucher();
                voucher.setNumber(
                                String.format("REC-%d-%05d",
                                                java.time.LocalDate.now().getYear(),
                                                donation.getId()));
                voucher.setEmailSend(donation.getPayerEmail());
                voucher.setFilePath("/api/donations/" + donation.getId());
                voucher.setTransaction(transaction);

                donationRepository.save(donation);
                transactionRepository.save(transaction);
                campaignRepository.save(campaign);
                voucherRepository.save(voucher);
        }

        @Transactional
        public void attachPayPalOrder(Long donationId, String orderId,
                        BigDecimal amountUsd, BigDecimal rate) {

                Donation donation = donationRepository
                                .findLockedById(donationId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Donación no encontrada"));

                if (donation.getStatus() != PaymentStatus.PENDING) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La donación no está pendiente");
                }

                donation.setPaypalOrderId(orderId);
                donation.setPaypalAmountUsd(amountUsd);
                donation.setPaypalExchangeRate(rate);
                donationRepository.save(donation);
        }

        @Transactional
        public void confirmPayPalPayment(Long donationId, String orderId,
                        String captureId, BigDecimal paidUsd,
                        String currency) {

                Donation donation = donationRepository
                                .findLockedById(donationId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Donación no encontrada"));

                if (donation.getStatus() == PaymentStatus.COMPLETED) {
                        if (orderId.equals(donation.getPaypalOrderId())) {
                                return;
                        }
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La donación ya tiene otro pago confirmado");
                }

                if (donation.getStatus() != PaymentStatus.PENDING) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La donación no está pendiente");
                }

                if (donation.getPaypalOrderId() != null
                                && !donation.getPaypalOrderId().equals(orderId)) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La orden de PayPal no corresponde a la donación");
                }

                if (!"USD".equals(currency)
                                || donation.getPaypalAmountUsd() == null
                                || donation.getPaypalAmountUsd().compareTo(paidUsd) != 0) {
                        throw new BusinessException(
                                        HttpStatus.BAD_REQUEST,
                                        "Los datos del pago no coinciden con la donación");
                }

                Transaction transaction = transactionRepository
                                .findFirstByDonation_IdOrderByTransactionDateDesc(donationId)
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Transacción no encontrada"));

                if (transaction.getMethodPay() != Method.PAYPAL) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La transacción no corresponde a PayPal");
                }

                if (donation.getCampaign() == null) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La donación no tiene campaña asociada");
                }

                Campaign campaign = campaignRepository
                                .findLockedById(donation.getCampaign().getId())
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Campaña no encontrada"));

                donation.setPaypalOrderId(orderId);
                donation.setPaypalCaptureId(captureId);
                donation.setStatus(PaymentStatus.COMPLETED);

                transaction.setStatus(PaymentStatus.COMPLETED);

                BigDecimal collected = campaign.getAmountCollected() != null
                                ? campaign.getAmountCollected()
                                : BigDecimal.ZERO;

                campaign.setAmountCollected(collected.add(donation.getAmount()));

                Voucher voucher = new Voucher();
                voucher.setNumber(
                                String.format("REC-%d-%05d",
                                                java.time.LocalDate.now().getYear(),
                                                donation.getId()));
                voucher.setEmailSend(donation.getPayerEmail());
                voucher.setFilePath("/api/donations/" + donation.getId());
                voucher.setTransaction(transaction);

                donationRepository.save(donation);
                transactionRepository.save(transaction);
                campaignRepository.save(campaign);
                voucherRepository.save(voucher);
        }
}
