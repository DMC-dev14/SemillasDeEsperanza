package com.donation.app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donation.app.dto.request.CampaignRequest;
import com.donation.app.dto.response.CampaignResponse;
import com.donation.app.enums.CampaignStatus;
import com.donation.app.security.CustomUserDetailsService.CustomUserDetails;
import com.donation.app.service.CampaignService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    
    // PÚBLICO - Listar campañas activas
    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getAllPublic() {
        return ResponseEntity.ok(campaignService.getActive());
    }

    // PÚBLICO - Listar campañas activas
    @GetMapping("/active")
    public ResponseEntity<List<CampaignResponse>> getActive() {
        return ResponseEntity.ok(campaignService.getActive());
    }

    // PÚBLICO - Ver detalle de campaña
    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    // SOLO ADMIN - Ver TODAS (incluidas DRAFT/CANCELLED/COMPLETED)
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<CampaignResponse>> getAllForAdmin() {
        return ResponseEntity.ok(campaignService.getAll());
    }

    // SOLO ADMIN - Crear
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CampaignRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.create(request, principal != null ? principal.getUser() : null));
    }

    // SOLO ADMIN - Editar completo
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CampaignResponse> update(@PathVariable Long id,
            @Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    // SOLO ADMIN - Cambiar solo estado
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CampaignResponse> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new com.donation.app.exception.BusinessException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }
        CampaignStatus status = CampaignStatus.valueOf(statusStr);
        return ResponseEntity.ok(campaignService.updateStatus(id, status));
    }

    // SOLO ADMIN - Eliminar
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
