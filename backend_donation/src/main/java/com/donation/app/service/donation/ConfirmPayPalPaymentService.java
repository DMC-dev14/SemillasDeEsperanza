package com.donation.app.service.donation;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class ConfirmPayPalPaymentService {

    private static final String USD = "USD";
    private static final String PEN = "PEN";
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("3.75");

    private final PayPalHttpClient payPalHttpClient;
    private final DonationRepository donationRepository;
    private final TransactionRepository transactionRepository;
    private final CampaignRepository campaignRepository;
    private final VoucherRepository voucherRepository;

    @Value("${paypal.usd-rate:3.75}")
    private BigDecimal usdRate;

    @Value("${paypal.return-url:}")
    private String returnUrl;

    @Value("${paypal.cancel-url:}")
    private String cancelUrl;

    @Transactional
    public void attachPayPalOrder(Long donationId, String orderId, BigDecimal amountUsd, BigDecimal rate) {

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
    public void confirmPayPalPayment(Long donationId, String orderId, String captureId, BigDecimal paidUsd, String currency) {

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
                    "La orden no corresponde a esta donación");
        }

        if (!USD.equals(currency)
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

        confirmPayPalPayment(
                donation.getId(), order.id(), capture.id(), paid, currency);
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

    private String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return "Donación";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}