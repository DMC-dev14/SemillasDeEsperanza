
package com.donation.app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.donation.app.dto.request.DonationRequest;
import com.donation.app.dto.response.DonationCreatedResponse;
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

    // CREAR DONACIÓN PENDIENTE
    @PostMapping
    public ResponseEntity<DonationCreatedResponse> create(
            @Valid @RequestBody DonationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        DonationCreatedResponse response =
                donationService.createDonation(
                        request,
                        principal != null ? principal.getUser() : null
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // DONACIONES DEL USUARIO
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyDonations(
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (principal == null) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "No autenticado"
            );
        }

        Long userId = principal.getUser().getId();

        return ResponseEntity.ok(Map.of(
                "summary", donationService.getClientDashboardStats(userId),
                "donations", donationService.getMyDonations(userId),
                "topDonors", donationService.getTopDonors(),
                "recentFeed", donationService.getRecentCompletedDonations()
        ));
    }

    // DONACIONES PÚBLICAS
    @GetMapping("/public-feed")
    public ResponseEntity<Map<String, Object>> getPublicFeed() {

        return ResponseEntity.ok(Map.of(
                "topDonors", donationService.getTopDonors(),
                "recentFeed", donationService.getRecentCompletedDonations()
        ));
    }

    // ADMIN - TODAS LAS DONACIONES
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<DonationResponse>> getAll() {

        return ResponseEntity.ok(
                donationService.getAllDonations()
        );
    }

    // ADMIN - CAMBIAR ESTADO
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DonationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String statusValue = body.get("status");

        if (statusValue == null || statusValue.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio"
            );
        }

        PaymentStatus status;

        try {
            status = PaymentStatus.valueOf(statusValue);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Estado de pago inválido"
            );
        }

        return ResponseEntity.ok(
                donationService.updateStatus(id, status)
        );
    }
}
