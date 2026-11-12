package com.donation.app.service.donation;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donation.app.dto.request.DonationRequest;
import com.donation.app.dto.response.DonationCreatedResponse;
import com.donation.app.enums.CampaignStatus;
import com.donation.app.enums.Method;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.Campaign;
import com.donation.app.model.Donation;
import com.donation.app.model.Transaction;
import com.donation.app.model.User;
import com.donation.app.repository.CampaignRepository;
import com.donation.app.repository.DonationRepository;
import com.donation.app.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateDonationService {

    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;
    private final TransactionRepository transactionRepository;
    private final DonationMapperService mapper;

    @Transactional
    public DonationCreatedResponse createDonation(DonationRequest request, User authenticatedUser) {

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

        Method method = mapper.parseMethod(request.paymentMethod());

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
                .methodPay(mapper.parseMethod(request.paymentMethod()))
                .amount(request.amount())
                .currency("PEN")
                .status(PaymentStatus.PENDING)
                .donation(savedDonation)
                .build();

        Transaction savedTx = transactionRepository.save(transaction);

        return new DonationCreatedResponse(
                mapper.mapToResponse(
                        savedDonation,
                        savedTx,
                        null,
                        donorName,
                        donorEmail),
                savedDonation.getCheckoutToken());
    }
}