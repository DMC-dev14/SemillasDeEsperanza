package com.donation.app.examplePayPal.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.donation.app.dto.response.DonationResponse;
import com.donation.app.enums.Method;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.examplePayPal.dtos.CreateOrderPayPalRequest;
import com.donation.app.examplePayPal.dtos.CreateOrderPayPalResponse;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.Donation;
import com.donation.app.model.Transaction;
import com.donation.app.repository.DonationRepository;
import com.donation.app.repository.TransactionRepository;
import com.donation.app.service.DonationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpRequest;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.AmountWithBreakdown;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.Capture;
import com.paypal.orders.LinkDescription;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.OrdersGetRequest;
import com.paypal.orders.PurchaseUnit;
import com.paypal.orders.PurchaseUnitRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayPalClientService {

    private static final String USD = "USD";
    private static final String PEN = "PEN";
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("3.75");

    private final PayPalHttpClient payPalHttpClient;
    private final DonationRepository donationRepository;
    private final TransactionRepository transactionRepository;
    private final DonationService donationService;

    @Value("${paypal.usd-rate:3.75}")
    private BigDecimal usdRate;

    @Value("${paypal.return-url:}")
    private String returnUrl;

    @Value("${paypal.cancel-url:}")
    private String cancelUrl;

    // ==================== 1. CREAR ORDEN ====================

    public CreateOrderPayPalResponse createOrder(CreateOrderPayPalRequest request) {

        Donation donation = loadDonation(request.donationId(), request.checkoutToken());

        if (donation.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "La donación no está pendiente");
        }

        BigDecimal rate = (usdRate != null && usdRate.signum() > 0) ? usdRate : DEFAULT_RATE;
        BigDecimal amountUsd = donation.getAmount().divide(rate, 2, RoundingMode.HALF_UP);

        if (amountUsd.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El monto es demasiado bajo para PayPal (mínimo $0.01)");
        }

        AmountWithBreakdown amount = new AmountWithBreakdown()
                .currencyCode(USD)
                .value(amountUsd.toPlainString());

        PurchaseUnitRequest unit = new PurchaseUnitRequest()
                .amountWithBreakdown(amount)
                .customId(donation.getId().toString())
                .invoiceId("DON-" + donation.getId())
                .description(truncate(donation.getMessage(), 127));

        ApplicationContext context = new ApplicationContext()
                .brandName("Semillas de Esperanza")
                .landingPage("LOGIN")
                .userAction("PAY_NOW");

        if (returnUrl != null && !returnUrl.isBlank()) {
            context.returnUrl(returnUrl);
        }
        if (cancelUrl != null && !cancelUrl.isBlank()) {
            context.cancelUrl(cancelUrl);
        }

        OrderRequest orderRequest = new OrderRequest()
                .checkoutPaymentIntent("CAPTURE")
                .purchaseUnits(List.of(unit))
                .applicationContext(context);

        OrdersCreateRequest createRequest = new OrdersCreateRequest()
                .prefer("return=representation")
                .requestBody(orderRequest);

        createRequest.header("PayPal-Request-Id", "donation-" + donation.getId());

        Order order = execute(createRequest);

        String approvalUrl = order.links() == null ? null
                : order.links().stream()
                        .filter(link -> "approve".equals(link.rel()))
                        .map(LinkDescription::href)
                        .findFirst()
                        .orElse(null);

        if (approvalUrl == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "PayPal no devolvió la URL de aprobación");
        }

        donationService.attachPayPalOrder(donation.getId(), order.id(), amountUsd, rate);

        return CreateOrderPayPalResponse.builder()
                .orderId(order.id())
                .status(order.status())
                .approvalUrl(approvalUrl)
                .amountUsd(amountUsd)
                .build();
    }

    // ==================== 2. CAPTURAR ====================

    public DonationResponse captureOrder(String orderId, Long donationId, String checkoutToken) {

        Donation donation = loadDonation(donationId, checkoutToken);

        if (donation.getStatus() == PaymentStatus.COMPLETED) {
            if (orderId.equals(donation.getPaypalOrderId())) {
                return donationService.mapToResponse(donation);
            }
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La donación ya tiene otro pago confirmado");
        }

        if (donation.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "La donación no está pendiente");
        }

        if (donation.getPaypalOrderId() != null
                && !donation.getPaypalOrderId().equals(orderId)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La orden no corresponde a esta donación");
        }

        OrdersCaptureRequest captureRequest = new OrdersCaptureRequest(orderId)
                .payPalRequestId("capture-donation-" + donation.getId());

        Order order = execute(captureRequest);

        confirmFromOrder(order, donation);

        Donation updated = donationRepository.findById(donation.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Donación no encontrada"));

        return donationService.mapToResponse(updated);
    }

    // ==================== 3. WEBHOOK ====================

    public void handleWebhook(JsonNode body) {

        String eventType = body.path("event_type").asText("");

        if (!"PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
            return;
        }

        String orderId = body.path("resource")
                .path("supplementary_data")
                .path("related_ids")
                .path("order_id")
                .asText("");

        if (orderId.isBlank()) {
            return;
        }

        Donation donation = donationRepository.findByPaypalOrderId(orderId).orElse(null);

        if (donation == null || donation.getStatus() == PaymentStatus.COMPLETED) {
            return;
        }

        Order order = execute(new OrdersGetRequest(orderId));

        confirmFromOrder(order, donation);
    }

    // ==================== VERIFICACIÓN ====================

    private void confirmFromOrder(Order order, Donation donation) {

        if (!"COMPLETED".equals(order.status())) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "PayPal no completó el pago (estado: " + order.status() + ")");
        }

        PurchaseUnit unit = (order.purchaseUnits() == null || order.purchaseUnits().isEmpty())
                ? null
                : order.purchaseUnits().get(0);

        if (unit == null
                || unit.payments() == null
                || unit.payments().captures() == null
                || unit.payments().captures().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "PayPal no devolvió los datos de la captura");
        }

        Capture capture = unit.payments().captures().get(0);

        if (capture.amount() == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "PayPal no devolvió el importe de la captura");
        }

        if (!donation.getId().toString().equals(unit.customId())) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "La orden no corresponde a esta donación");
        }

        String currency = capture.amount().currencyCode();
        BigDecimal paid;

        try {
            paid = new BigDecimal(capture.amount().value());
        } catch (RuntimeException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Importe del pago no válido");
        }

        if (!USD.equals(currency)) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "La moneda del pago no es USD");
        }

        if (donation.getPaypalAmountUsd() == null
                || donation.getPaypalAmountUsd().compareTo(paid) != 0) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "El monto pagado no coincide con la donación");
        }

        donationService.confirmPayPalPayment(
                donation.getId(), order.id(), capture.id(), paid, currency);
    }

    // ==================== HELPERS ====================

    private Donation loadDonation(Long donationId, String checkoutToken) {

        if (donationId == null || checkoutToken == null || checkoutToken.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Datos de pago incompletos");
        }

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Donación no encontrada"));

        if (!Objects.equals(donation.getCheckoutToken(), checkoutToken)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "No autorizado para pagar esta donación");
        }

        if (!PEN.equals(donation.getCurrency())
                || donation.getAmount() == null
                || donation.getAmount().signum() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Monto o moneda no válidos");
        }

        Transaction transaction = transactionRepository
                .findFirstByDonation_IdOrderByTransactionDateDesc(donationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Transacción no encontrada"));

        if (transaction.getMethodPay() != Method.PAYPAL) {
            throw new BusinessException(HttpStatus.CONFLICT, "La donación no utiliza PayPal");
        }

        return donation;
    }

    private String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return "Donación";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private <T> T execute(HttpRequest<T> request) {
        try {
            return payPalHttpClient.execute(request).result();
        } catch (HttpException e) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "PayPal rechazó la operación (" + e.statusCode() + "): " + e.getMessage());
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Error comunicándose con PayPal: " + e.getMessage());
        }
    }
}