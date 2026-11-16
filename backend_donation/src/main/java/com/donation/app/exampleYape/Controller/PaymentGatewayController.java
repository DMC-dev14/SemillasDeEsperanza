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

    // 1) Teléfono + OTP -> token de Yape
    @PostMapping("/yape/token")
    public Mono<ResponseEntity<YapeTokenResponse>> yapeToken(
            @Valid @RequestBody YapeTokenRequestDTO body) {

        return yapeService
                .tokenize(body)
                .map(ResponseEntity::ok);
    }

    // 2) Token -> cobro
    @PostMapping("/charge-payment")
    public Mono<ResponseEntity<ChargePaymentResponse>> chargePayment(
            @Valid @RequestBody ChargePaymentRequestDTO body) {

        return yapeService
                .chargePayment(body)
                .map(ResponseEntity::ok);
    }
}