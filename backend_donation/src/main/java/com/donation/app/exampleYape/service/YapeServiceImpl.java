package com.donation.app.exampleYape.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.donation.app.enums.Method;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.exampleYape.model.*;
import com.donation.app.model.Donation;
import com.donation.app.model.Transaction;
import com.donation.app.repository.DonationRepository;
import com.donation.app.repository.TransactionRepository;
import com.donation.app.service.DonationService;
import com.donation.app.exception.BusinessException;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class YapeServiceImpl implements YapeService {

        private final WebClient mercadoPagoWebClient;
        private final DonationRepository donationRepository;
        private final TransactionRepository transactionRepository;
        private final DonationService donationService;

        @Value("${mercadopago.access-token:}")
        private String accessToken;

        @Value("${mercadopago.public-key:}")
        private String publicKey;

        // ==================== 1. TOKENIZAR YAPE ====================
        @Override
        public Mono<YapeTokenResponse> tokenize(YapeTokenRequestDTO body) {

                return Mono.fromCallable(() -> validarTokenizacion(body))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(data -> {

                                        if (publicKey == null || publicKey.isBlank()) {
                                                return Mono.error(new BusinessException(
                                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                                "La pasarela de pagos no está disponible"));
                                        }

                                        YapeGenerateTokenRequest request = YapeGenerateTokenRequest.builder()
                                                        .phoneNumber(data.phoneNumber())
                                                        .otp(data.otp())
                                                        .requestId(UUID.randomUUID().toString())
                                                        .build();

                                        return mercadoPagoWebClient.post()
                                                        .uri(uriBuilder -> uriBuilder
                                                                        .path("/platforms/pci/yape/v1/payment")
                                                                        .queryParam("public_key", publicKey)
                                                                        .build())
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .bodyValue(request)
                                                        .retrieve()
                                                        .bodyToMono(YapeGenerateTokenResponse.class)
                                                        .map(response -> {

                                                                if (response.getId() == null
                                                                                || response.getId().isBlank()) {
                                                                        throw new BusinessException(
                                                                                        HttpStatus.BAD_GATEWAY,
                                                                                        "Mercado Pago no devolvió el token de Yape");
                                                                }

                                                                return new YapeTokenResponse(response.getId());
                                                        });
                                });
        }

        private TokenData validarTokenizacion(YapeTokenRequestDTO body) {

                Donation donation = donationRepository
                                .findById(body.getDonationId())
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Donación no encontrada"));

                if (!Objects.equals(
                                donation.getCheckoutToken(),
                                body.getCheckoutToken())) {
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
                                .findFirstByDonation_IdOrderByTransactionDateDesc(
                                                donation.getId())
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Transacción no encontrada"));

                if (transaction.getMethodPay() != Method.YAPE) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La donación no utiliza Yape");
                }

                return new TokenData(body.getPhoneNumber(), body.getOtp());
        }

        private record TokenData(String phoneNumber, String otp) {
        }

        // ==================== 2. COBRAR ====================
        @Override
        public Mono<ChargePaymentResponse> chargePayment(
                        ChargePaymentRequestDTO body) {

                return Mono.fromCallable(() -> buscarDonacion(body))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(data -> {

                                        if (accessToken == null || accessToken.isBlank()) {
                                                return Mono.error(new BusinessException(
                                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                                "La pasarela de pagos no está disponible"));
                                        }

                                        ChargePaymentRequest request = ChargePaymentRequest.builder()
                                                        .token(body.getToken())
                                                        .transactionAmount(data.amount())
                                                        .description("Donación #" + data.donationId())
                                                        .installments(1)
                                                        .paymentMethodId("yape")
                                                        .externalReference(data.donationId().toString())
                                                        .payer(ChargePaymentPayerRequest.builder()
                                                                        .email(data.email())
                                                                        .build())
                                                        .build();

                                        return mercadoPagoWebClient.post()
                                                        .uri("/v1/payments")
                                                        .headers(headers -> headers.setBearerAuth(accessToken))
                                                        .header("X-Idempotency-Key", data.idempotencyKey())
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .bodyValue(request)
                                                        .retrieve()
                                                        .bodyToMono(ChargePaymentResponse.class)
                                                        .flatMap(response -> {

                                                                if (!"approved".equals(response.getStatus())) {
                                                                        return Mono.just(response);
                                                                }

                                                                return verificarPago(data, response);
                                                        });
                                });
        }

        private PaymentData buscarDonacion(ChargePaymentRequestDTO body) {

                if (body.getDonationId() == null ||
                                body.getToken() == null ||
                                body.getToken().isBlank() ||
                                body.getCheckoutToken() == null ||
                                body.getCheckoutToken().isBlank()) {
                        throw new BusinessException(
                                        HttpStatus.BAD_REQUEST,
                                        "Datos de pago incompletos");
                }

                Donation donation = donationRepository
                                .findById(body.getDonationId())
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Donación no encontrada"));

                if (!Objects.equals(
                                donation.getCheckoutToken(),
                                body.getCheckoutToken())) {
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
                                .findFirstByDonation_IdOrderByTransactionDateDesc(
                                                donation.getId())
                                .orElseThrow(() -> new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "Transacción no encontrada"));

                if (transaction.getMethodPay() != Method.YAPE) {
                        throw new BusinessException(
                                        HttpStatus.CONFLICT,
                                        "La donación no utiliza Yape");
                }

                if (!"PEN".equals(donation.getCurrency()) ||
                                donation.getAmount() == null ||
                                donation.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessException(
                                        HttpStatus.BAD_REQUEST,
                                        "Monto o moneda no válidos");
                }

                if (donation.getPayerEmail() == null ||
                                donation.getPayerEmail().isBlank()) {
                        throw new BusinessException(
                                        HttpStatus.BAD_REQUEST,
                                        "Falta el correo del donante");
                }

                return new PaymentData(
                                donation.getId(),
                                donation.getAmount(),
                                donation.getPayerEmail(),
                                body.getToken());
        }

        private Mono<ChargePaymentResponse> verificarPago(
                        PaymentData data,
                        ChargePaymentResponse response) {

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

                                        return Mono.fromRunnable(() -> donationService.confirmYapePayment(
                                                        data.donationId(),
                                                        paymentId,
                                                        amount,
                                                        currency))
                                                        .subscribeOn(Schedulers.boundedElastic())
                                                        .thenReturn(response);
                                });
        }

        private record PaymentData(
                        Long donationId,
                        BigDecimal amount,
                        String email,
                        String idempotencyKey) {
        }
}