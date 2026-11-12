package com.donation.app.service.donation;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donation.app.dto.response.DonationResponse;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateDonationStatusService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final DonationMapperService mapper;

    @Transactional
    public DonationResponse updateStatus(Long donationId, PaymentStatus newStatus) {

        if (newStatus == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio");
        }

        Donation donation = donationRepository
                .findLockedById(donationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Donación no encontrada"));

        Transaction transaction = transactionRepository
                .findFirstByDonation_IdOrderByTransactionDateDesc(donationId)
                .orElse(null);

        // Impedir cambios manuales en pagos gestionados por pasarela
        if (transaction != null &&
                (transaction.getMethodPay() == Method.YAPE
                        || transaction.getMethodPay() == Method.PAYPAL)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "El estado de pagos " + transaction.getMethodPay().name()
                            + " debe verificarse con la pasarela");
        }

        PaymentStatus oldStatus = donation.getStatus();

        if (oldStatus != newStatus) {

            Campaign campaign = donation.getCampaign();

            if (campaign != null) {
                campaign = campaignRepository
                        .findLockedById(campaign.getId())
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "Campaña no encontrada"));

                BigDecimal current = campaign.getAmountCollected() != null
                        ? campaign.getAmountCollected()
                        : BigDecimal.ZERO;

                if (oldStatus == PaymentStatus.COMPLETED &&
                        newStatus != PaymentStatus.COMPLETED) {

                    campaign.setAmountCollected(
                            current.subtract(donation.getAmount())
                                    .max(BigDecimal.ZERO));

                } else if (oldStatus != PaymentStatus.COMPLETED &&
                        newStatus == PaymentStatus.COMPLETED) {

                    campaign.setAmountCollected(
                            current.add(donation.getAmount()));
                }

                campaignRepository.save(campaign);
            }

            donation.setStatus(newStatus);
            donationRepository.save(donation);

            if (transaction != null) {
                transaction.setStatus(newStatus);
                transactionRepository.save(transaction);
            }

            // Crear voucher si se completa
            if (newStatus == PaymentStatus.COMPLETED && transaction != null) {
                Voucher voucher = new Voucher();
                voucher.setNumber(
                        String.format("REC-%d-%05d",
                                java.time.LocalDate.now().getYear(),
                                donation.getId()));
                voucher.setEmailSend(donation.getPayerEmail());
                voucher.setFilePath("/api/donations/" + donation.getId());
                voucher.setTransaction(transaction);
                voucherRepository.save(voucher);
            }
        }

        return mapper.mapToResponse(donation);
    }
}