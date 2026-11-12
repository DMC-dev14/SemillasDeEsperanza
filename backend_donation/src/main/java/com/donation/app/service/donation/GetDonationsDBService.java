package com.donation.app.service.donation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.donation.app.dto.response.DonationResponse;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.model.Donation;
import com.donation.app.repository.DonationRepository;
import com.donation.app.repository.TransactionRepository;
import com.donation.app.repository.VoucherRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDonationsDBService {

    private final DonationRepository donationRepository;
    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final DonationMapperService mapper;

    public List<DonationResponse> getMyDonations(Long userId) {
        return donationRepository.findByDonor_IdOrderByDonationDateDesc(userId).stream()
                .map(mapper::mapToResponse)
                .toList();
    }

    public List<DonationResponse> getAllDonations() {
        return donationRepository.findAllByOrderByDonationDateDesc().stream()
                .map(mapper::mapToResponse)
                .toList();
    }

    public List<DonationResponse> getRecentCompletedDonations() {
        return donationRepository.findTop10ByStatusOrderByDonationDateDesc(PaymentStatus.COMPLETED).stream()
                .map(mapper::mapToResponse)
                .toList();
    }

    public Map<String, Object> getClientDashboardStats(Long userId) {
        List<Donation> myDonations = donationRepository.findByDonor_IdOrderByDonationDateDesc(userId);
        BigDecimal totalDonado = myDonations.stream()
                .filter(d -> d.getStatus() == PaymentStatus.COMPLETED)
                .map(Donation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedCount = myDonations.stream()
                .filter(d -> d.getStatus() == PaymentStatus.COMPLETED)
                .count();

        long distinctCampaigns = myDonations.stream()
                .filter(d -> d.getStatus() == PaymentStatus.COMPLETED && d.getCampaign() != null)
                .map(d -> d.getCampaign().getId())
                .distinct()
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDonado", totalDonado);
        summary.put("cantidadDonaciones", completedCount);
        summary.put("proyectosApoyados", distinctCampaigns);
        return summary;
    }

    public List<Map<String, Object>> getTopDonors() {
        List<Donation> allCompleted = donationRepository.findAllByOrderByDonationDateDesc().stream()
                .filter(d -> d.getStatus() == PaymentStatus.COMPLETED)
                .toList();

        Map<String, BigDecimal> totalsByDonor = new LinkedHashMap<>();
        Map<String, Integer> countsByDonor = new LinkedHashMap<>();
        Map<String, String> displayNames = new LinkedHashMap<>();

        for (Donation d : allCompleted) {
            String key;
            String name;
            if (d.getDonor() != null) {
                key = "USER_" + d.getDonor().getId();
                name = formatShortName(d.getDonor().getName());
            } else {
                key = "ANON_" + d.getId();
                name = extractDonorNameFromMessage(d.getMessage());
            }
            totalsByDonor.merge(key, d.getAmount(), BigDecimal::add);
            countsByDonor.merge(key, 1, Integer::sum);
            displayNames.putIfAbsent(key, name);
        }

        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(totalsByDonor.entrySet());
        sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        String[] badges = { "🥇", "🥈", "🥉", "#4", "#5" };
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(5, sorted.size());
        for (int i = 0; i < limit; i++) {
            String key = sorted.get(i).getKey();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", i + 1);
            row.put("badge", badges[i]);
            row.put("name", displayNames.getOrDefault(key, "Donante Solidario"));
            row.put("donations", countsByDonor.getOrDefault(key, 1));
            row.put("total", sorted.get(i).getValue());
            result.add(row);
        }
        return result;
    }

    // Copia de helpers privados del mapper (para no depender de instancias privadas)
    private String extractDonorNameFromMessage(String msg) {
        if (msg != null && msg.contains(" - ")) {
            String[] parts = msg.split(" - ");
            String candidate = parts[parts.length - 1].trim();
            if (!candidate.isEmpty())
                return candidate;
        }
        return "Donante Solidario";
    }

    private String formatShortName(String fullName) {
        if (fullName == null || fullName.isBlank())
            return "Donante";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0];
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }
}