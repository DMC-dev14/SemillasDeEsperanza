package com.donation.app.exampleYape.Controller;

import com.donation.app.exampleYape.model.ChargePaymentRequestDTO;
import com.donation.app.exampleYape.model.ChargePaymentResponse;
import com.donation.app.exampleYape.model.YapeTokenRequestDTO;
import com.donation.app.exampleYape.model.YapeTokenResponse;
import com.donation.app.exampleYape.service.YapeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payment-gateway")
@RequiredArgsConstructor
public class PaymentGatewayController {

    private final YapeService yapeService;

    @PostMapping("/yape/token")
    public Mono<ResponseEntity<YapeTokenResponse>> yapeToken(
            @Valid @RequestBody YapeTokenRequestDTO body) {

        return yapeService
                .tokenize(body)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/charge-payment")
    public Mono<ResponseEntity<ChargePaymentResponse>> chargePayment(
            @Valid @RequestBody ChargePaymentRequestDTO body,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {

        if (body.getIpAddress() == null || body.getIpAddress().isBlank()) {
            String candidate = forwardedFor != null && !forwardedFor.isBlank()
                    ? forwardedFor.split(",")[0].trim()
                    : realIp;
            if (candidate != null && candidate.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                body.setIpAddress(candidate);
            }
        }

        return yapeService
                .chargePayment(body)
                .map(ResponseEntity::ok);
    }
}