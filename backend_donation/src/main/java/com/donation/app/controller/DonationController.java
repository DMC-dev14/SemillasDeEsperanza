package com.donation.app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donation.app.dto.request.DonationRequest;
import com.donation.app.dto.response.DonationResponse;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.exception.BusinessException;
import com.donation.app.security.CustomUserDetailsService.CustomUserDetails;
import com.donation.app.service.DonationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    public ResponseEntity<DonationResponse> create(@Valid @RequestBody DonationRequest request,
                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.createDonation(request, principal != null ? principal.getUser() : null));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyDonations(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        Long userId = principal.getUser().getId();
        return ResponseEntity.ok(Map.of(
                "summary", donationService.getClientDashboardStats(userId),
                "donations", donationService.getMyDonations(userId),
                "topDonors", donationService.getTopDonors(),
                "recentFeed", donationService.getRecentCompletedDonations()
        ));
    }

    @GetMapping("/public-feed")
    public ResponseEntity<Map<String, Object>> getPublicFeed() {
        return ResponseEntity.ok(Map.of(
                "topDonors", donationService.getTopDonors(),
                "recentFeed", donationService.getRecentCompletedDonations()
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<DonationResponse>> getAll() {
        return ResponseEntity.ok(donationService.getAllDonations());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DonationResponse> updateStatus(@PathVariable Long id,
                                                         @RequestBody Map<String, String> body) {
        PaymentStatus status = PaymentStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(donationService.updateStatus(id, status));
    }
}
