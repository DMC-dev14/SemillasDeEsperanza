package com.donation.app.examplePayPal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.donation.app.dto.response.DonationResponse;
import com.donation.app.examplePayPal.dtos.CaptureOrderPayPalRequest;
import com.donation.app.examplePayPal.dtos.CreateOrderPayPalRequest;
import com.donation.app.examplePayPal.dtos.CreateOrderPayPalResponse;
import com.donation.app.examplePayPal.service.PayPalClientService;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/paypal")
@RequiredArgsConstructor
@Tag(name = "PayPal", description = "Checkout API de PayPal")
public class PayPalClientController {

    private final PayPalClientService payPalClientService;

    @PostMapping("/orders")
    public ResponseEntity<CreateOrderPayPalResponse> createOrder(
            @Valid @RequestBody CreateOrderPayPalRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payPalClientService.createOrder(request));
    }

    @PostMapping("/orders/{id}/capture")
    public ResponseEntity<DonationResponse> captureOrder(
            @PathVariable("id") String orderId,
            @Valid @RequestBody CaptureOrderPayPalRequest request) {

        return ResponseEntity.ok(payPalClientService.captureOrder(
                orderId, request.donationId(), request.checkoutToken()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody(required = false) JsonNode body) {

        if (body != null) {
            payPalClientService.handleWebhook(body);
        }
        return ResponseEntity.ok().build();
    }
}