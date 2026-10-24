package com.donation.app.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donation.app.dto.request.CampaignRequest;
import com.donation.app.dto.response.CampaignResponse;
import com.donation.app.enums.CampaignStatus;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.Campaign;
import com.donation.app.model.User;
import com.donation.app.repository.CampaignRepository;
import com.donation.app.repository.DonationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;

    public List<CampaignResponse> getAll() {
        return campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "createDate")).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CampaignResponse> getActive() {
        return campaignRepository.findByStatusOrderByCreateDateDesc(CampaignStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CampaignResponse getById(Long id) {
        Campaign campaign = getEntity(id);
        return mapToResponse(campaign);
    }

    public Campaign getEntity(Long id) {
        Campaign c = campaignRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Campaña no encontrada"));
        long count = donationRepository.countByCampaign_IdAndStatus(c.getId(), PaymentStatus.COMPLETED);
        c.setDonorsCount(count);
        return c;
    }

    @Transactional
    public CampaignResponse create(CampaignRequest request, User creator) {
        Campaign campaign = Campaign.builder()
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : "")
                .history(request.history() != null && !request.history().isBlank()
                        ? request.history().trim()
                        : request.description().trim())
                .imageUrl(request.imageUrl() != null && !request.imageUrl().isBlank()
                        ? request.imageUrl().trim()
                        : "https://images.unsplash.com/photo-1641736047654-48c2546fcf19?w=800&h=450&fit=crop&auto=format")
                .targetAmount(request.targetAmount())
                .amountCollected(BigDecimal.ZERO)
                .status(request.status() != null ? request.status() : CampaignStatus.ACTIVE)
                .startDate(request.startDate() != null ? request.startDate() : LocalDateTime.now())
                .endDate(request.endDate())
                .creator(creator)
                .build();

        return mapToResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse update(Long id, CampaignRequest request) {
        Campaign campaign = getEntity(id);

        if (request.title() != null && !request.title().isBlank()) {
            campaign.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            campaign.setDescription(request.description().trim());
        }
        if (request.history() != null) {
            campaign.setHistory(request.history().trim());
        }
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            campaign.setImageUrl(request.imageUrl().trim());
        }
        if (request.targetAmount() != null) {
            campaign.setTargetAmount(request.targetAmount());
        }
        if (request.status() != null) {
            campaign.setStatus(request.status());
        }
        if (request.startDate() != null) {
            campaign.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            campaign.setEndDate(request.endDate());
        }

        return mapToResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse updateStatus(Long id, CampaignStatus status) {
        if (status == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }
        Campaign campaign = getEntity(id);
        campaign.setStatus(status);
        return mapToResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public void delete(Long id) {
        Campaign campaign = getEntity(id);
        if (donationRepository.existsByCampaign_Id(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No se puede eliminar la campaña porque ya tiene donaciones registradas. Puedes cambiar su estado a CANCELLED o COMPLETED.");
        }
        campaignRepository.delete(campaign);
    }

    public CampaignResponse mapToResponse(Campaign c) {
        long dbDonors = donationRepository.countByCampaign_IdAndStatus(c.getId(), PaymentStatus.COMPLETED);
        c.setDonorsCount(dbDonors);
        return new CampaignResponse(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getHistory(),
                c.getImage(),
                c.getCategory(),
                c.getBadgeClass(),
                c.getLocation(),
                c.getGoal(),
                c.getRaised(),
                c.getPorcentaje(),
                c.getDonors(),
                c.getStatus(),
                c.getStartDate(),
                c.getEndDate(),
                c.getCreateDate(),
                c.getUpdateDate()
        );
    }
}
