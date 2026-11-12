package com.donation.app.service.donation;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.donation.app.enums.Method;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.Campaign;
import com.donation.app.model.Donation;
import com.donation.app.model.Transaction;
import com.donation.app.model.Voucher;
import com.donation.app.repository.CampaignRepository;
import com.donation.app.repository.DonationRepository;
import com.donation.app.repository.TransactionRepository;
import com.donation.app.repository.VoucherRepository;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class ConfirmYapePaymentService {

    private final WebClient mercadoPagoWebClient;
    private final DonationRepository donationRepository;
    private final TransactionRepository transactionRepository;
    private final CampaignRepository campaignRepository;
    private final VoucherRepository voucherRepository;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Transactional
    public void confirmYapePayment(Long donationId, String paymentId, BigDecimal paidAmount, String currency) {

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
                .findFirstByDonation_IdOrderByTransactionDateDesc(donationId)
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

    // Método para verificación reactiva (usado por YapeServiceImpl)
    public Mono<Void> verifyAndConfirmReactive(Long donationId, String token, String checkoutToken) {
        return Mono.fromCallable(() -> {
                    Donation donation = donationRepository
                            .findById(donationId)
                            .orElseThrow(() -> new BusinessException(
                                    HttpStatus.NOT_FOUND,
                                    "Donación no encontrada"));

                    if (!Objects.equals(donation.getCheckoutToken(), checkoutToken)) {
                        throw new BusinessException(
                                HttpStatus.FORBIDDEN,
                                "No autorizado para pagar esta donación");
                    }

                    if (donation.getStatus() != PaymentStatus.PENDING) {
                        throw new BusinessException(
                                HttpStatus.CONFLICT,
                                "La donación no está pendiente");
                    }

                    Transaction transaction = transactionRepository
                            .findFirstByDonation_IdOrderByTransactionDateDesc(donationId)
                            .orElseThrow(() -> new BusinessException(
                                    HttpStatus.NOT_FOUND,
                                    "Transacción no encontrada"));

                    if (transaction.getMethodPay() != Method.YAPE) {
                        throw new BusinessException(
                                HttpStatus.CONFLICT,
                                "La donación no utiliza Yape");
                    }

                    return new PaymentData(
                            donation.getId(),
                            donation.getAmount(),
                            donation.getPayerEmail(),
                            token);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(data -> {
                    if (accessToken == null || accessToken.isBlank()) {
                        return Mono.error(new BusinessException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "La pasarela de pagos no está disponible"));
                    }

                    com.donation.app.exampleYape.model.ChargePaymentRequest request = com.donation.app.exampleYape.model.ChargePaymentRequest.builder()
                            .token(data.token())
                            .transactionAmount(data.amount())
                            .description("Donación #" + data.donationId())
                            .installments(1)
                            .paymentMethodId("yape")
                            .externalReference(data.donationId().toString())
                            .payer(com.donation.app.exampleYape.model.ChargePaymentPayerRequest.builder()
                                    .email(data.email())
                                    .build())
                            .build();

                    return mercadoPagoWebClient.post()
                            .uri("/v1/payments")
                            .headers(headers -> headers.setBearerAuth(accessToken))
                            .header("X-Idempotency-Key", data.donationId().toString())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(com.donation.app.exampleYape.model.ChargePaymentResponse.class)
                            .flatMap(response -> {

                                if (!"approved".equals(response.getStatus())) {
                                    return Mono.empty();
                                }

                                return verifyPayment(data, response);
                            });
                })
                .then();
    }

    private Mono<Void> verifyPayment(PaymentData data, com.donation.app.exampleYape.model.ChargePaymentResponse response) {
        if (response.getId() == null || response.getId().isBlank()) {
            return Mono.error(new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Mercado Pago no devolvió un ID de pago"));
        }

        return mercadoPagoWebClient.get()
                .uri("/v1/payments/{id}", response.getId())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(payment -> {

                    String status = payment.path("status").asText();
                    String method = payment.path("payment_method_id").asText();
                    String reference = payment.path("external_reference").asText();
                    String currency = payment.path("currency_id").asText();
                    String paymentId = payment.path("id").asText();

                    BigDecimal amount;

                    try {
                        amount = new BigDecimal(
                                payment.path("transaction_amount").asText());
                    } catch (NumberFormatException e) {
                        return Mono.error(new BusinessException(
                                HttpStatus.BAD_GATEWAY,
                                "Importe del pago no válido"));
                    }

                    if (!"approved".equals(status) ||
                            !"yape".equals(method) ||
                            !"PEN".equals(currency) ||
                            !data.donationId().toString().equals(reference) ||
                            !response.getId().equals(paymentId) ||
                            amount.compareTo(data.amount()) != 0) {

                        return Mono.error(new BusinessException(
                                HttpStatus.BAD_GATEWAY,
                                "El pago no pudo verificarse"));
                    }

                    return Mono.fromRunnable(() -> confirmYapePayment(
                            data.donationId(),
                            paymentId,
                            amount,
                            currency))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then();
                });
    }

    private record PaymentData(
            Long donationId,
            BigDecimal amount,
            String email,
            String token) {
    }
}