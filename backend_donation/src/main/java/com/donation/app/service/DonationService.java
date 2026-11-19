package com.donation.app.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donation.app.dto.request.DonationRequest;
import com.donation.app.dto.response.DonationCreatedResponse;
import com.donation.app.dto.response.DonationResponse;
import com.donation.app.dto.response.PagedResponse;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.User;
import com.donation.app.service.donation.ConfirmPayPalPaymentService;
import com.donation.app.service.donation.ConfirmYapePaymentService;
import com.donation.app.service.donation.CreateDonationService;
import com.donation.app.service.donation.DonationMapperService;
import com.donation.app.service.donation.GetDonationsDBService;
import com.donation.app.service.donation.UpdateDonationStatusService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {

    private final CreateDonationService createDonationService;
    private final GetDonationsDBService getDonationsDBService;
    private final UpdateDonationStatusService updateDonationStatusService;
    private final ConfirmYapePaymentService confirmYapePaymentService;
    private final ConfirmPayPalPaymentService confirmPayPalPaymentService;
    private final DonationMapperService donationMapperService;

    @Transactional
    public DonationCreatedResponse createDonation(DonationRequest request, User authenticatedUser) {
        return createDonationService.createDonation(request, authenticatedUser);
    }

    public List<DonationResponse> getMyDonations(Long userId) {
        return getDonationsDBService.getMyDonations(userId);
    }

    public List<DonationResponse> getAllDonations() {
        return getDonationsDBService.getAllDonations();
    }

    public PagedResponse<DonationResponse> getDonationsPage(int page, int size) {
        return getDonationsDBService.getDonationsPage(page, size);
    }

    public PagedResponse<DonationResponse> getMyDonationsPage(Long userId, int page, int size) {
        return getDonationsDBService.getMyDonationsPage(userId, page, size);
    }

    public List<DonationResponse> getRecentCompletedDonations() {
        return getDonationsDBService.getRecentCompletedDonations();
    }

    public Map<String, Object> getClientDashboardStats(Long userId) {
        return getDonationsDBService.getClientDashboardStats(userId);
    }

    public List<Map<String, Object>> getTopDonors() {
        return getDonationsDBService.getTopDonors();
    }

    @Transactional
    public DonationResponse updateStatus(Long donationId, PaymentStatus newStatus) {
        return updateDonationStatusService.updateStatus(donationId, newStatus);
    }

    public DonationResponse mapToResponse(com.donation.app.model.Donation d) {
        return donationMapperService.mapToResponse(d);
    }

    @Transactional
    public void confirmYapePayment(Long donationId, String paymentId, BigDecimal paidAmount, String currency) {
        confirmYapePaymentService.confirmYapePayment(donationId, paymentId, paidAmount, currency);
    }

    public reactor.core.publisher.Mono<Void> verifyAndConfirmYapeReactive(Long donationId, String token, String checkoutToken) {
        return confirmYapePaymentService.verifyAndConfirmReactive(donationId, token, checkoutToken);
    }

    @Transactional
    public void attachPayPalOrder(Long donationId, String orderId, BigDecimal amountUsd, BigDecimal rate) {
        confirmPayPalPaymentService.attachPayPalOrder(donationId, orderId, amountUsd, rate);
    }

    @Transactional
    public void confirmPayPalPayment(Long donationId, String orderId, String captureId, BigDecimal paidUsd, String currency) {
        confirmPayPalPaymentService.confirmPayPalPayment(donationId, orderId, captureId, paidUsd, currency);
    }

    public void handlePayPalWebhook(com.fasterxml.jackson.databind.JsonNode body) {
        confirmPayPalPaymentService.handleWebhook(body);
    }
}