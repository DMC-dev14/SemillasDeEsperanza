
package com.donation.app.exampleYape.Controller;

import com.donation.app.service.DonationService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

@RestController
@RequestMapping("/api/payment-gateway")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final WebClient mercadoPagoWebClient;
    private final DonationService donationService;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public Mono<ResponseEntity<Void>> recibirNotificacion(
            @RequestParam(name = "data.id", required = false) String paymentId,

            @RequestParam(name = "type", required = false) String type,

            @RequestHeader(name = "x-signature", required = false) String signature,

            @RequestHeader(name = "x-request-id", required = false) String requestId) {

        if (accessToken.isBlank() || webhookSecret.isBlank()) {
            return Mono.just(
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }

        // Comprobar que la notificación es auténtica
        if (!firmaValida(paymentId, signature, requestId)) {
            return Mono.just(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        ResponseEntity<Void> ok = ResponseEntity.ok().build();

        if (!"payment".equals(type)) {
            return Mono.just(ok);
        }

        // Consultar directamente a Mercado Pago
        return mercadoPagoWebClient.get()
                .uri("/v1/payments/{id}", paymentId)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(payment -> {

                    if (!paymentId.equals(payment.path("id").asText())) {
                        return Mono.error(new IllegalStateException(
                                "El ID de pago no coincide"));
                    }

                    String status = payment.path("status").asText();
                    String method = payment.path("payment_method_id").asText();
                    String currency = payment.path("currency_id").asText();
                    String reference = payment.path("external_reference").asText();

                    // Solo confirmar pagos Yape aprobados
                    if (!"approved".equals(status) ||
                            !"yape".equals(method)) {
                        return Mono.just(ok);
                    }

                    if (!"PEN".equals(currency) ||
                            !reference.matches("[0-9]+") ||
                            !payment.path("transaction_amount").isNumber()) {

                        return Mono.error(new IllegalStateException(
                                "Datos de pago inválidos"));
                    }

                    Long donationId = Long.valueOf(reference);

                    BigDecimal amount = payment
                            .path("transaction_amount")
                            .decimalValue();

                    // Actualizar donación, campaña y comprobante
                    return Mono.fromRunnable(() -> donationService.confirmYapePayment(
                            donationId,
                            paymentId,
                            amount,
                            currency))
                            .subscribeOn(Schedulers.boundedElastic())
                            .thenReturn(ok);
                });
    }

    private boolean firmaValida(
            String paymentId,
            String signature,
            String requestId) {

        if (paymentId == null || paymentId.isBlank() ||
                signature == null || requestId == null ||
                requestId.isBlank()) {
            return false;
        }

        try {
            String ts = null;
            String v1 = null;

            for (String part : signature.split(",")) {
                String[] pair = part.trim().split("=", 2);

                if (pair.length == 2) {
                    if ("ts".equals(pair[0]))
                        ts = pair[1];
                    if ("v1".equals(pair[0]))
                        v1 = pair[1];
                }
            }

            if (ts == null || v1 == null)
                return false;

            String manifest = "id:" + paymentId.toLowerCase(Locale.ROOT) +
                    ";request-id:" + requestId +
                    ";ts:" + ts + ";";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));

            byte[] expected = mac.doFinal(
                    manifest.getBytes(StandardCharsets.UTF_8));

            byte[] received = HexFormat.of().parseHex(v1);

            return MessageDigest.isEqual(expected, received);

        } catch (Exception e) {
            return false;
        }
    }
}
