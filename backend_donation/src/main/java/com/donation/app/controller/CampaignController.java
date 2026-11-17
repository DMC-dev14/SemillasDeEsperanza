package com.donation.app.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.http.parser.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.donation.app.dto.request.CampaignRequest;
import com.donation.app.dto.response.CampaignResponse;
import com.donation.app.enums.CampaignStatus;
import com.donation.app.model.Campaign;
import com.donation.app.model.Image;
import com.donation.app.repository.CampaignRepository;
import com.donation.app.security.CustomUserDetailsService.CustomUserDetails;
import com.donation.app.service.CampaignService;
import com.donation.app.service.ImageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final ImageService imageService;
    private final CampaignRepository campaignRepository;

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getAllPublic() {
        return ResponseEntity.ok(campaignService.getActive());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CampaignResponse>> getActive() {
        return ResponseEntity.ok(campaignService.getActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<CampaignResponse>> getAllForAdmin() {
        return ResponseEntity.ok(campaignService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CampaignRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.create(request, principal != null ? principal.getUser() : null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CampaignResponse> update(@PathVariable Long id,
            @Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, String>> uploadCampaignImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) throws IOException {

        Campaign campaign = campaignService.getEntity(id);
        Image newImage = imageService.uploadImage(file);

        Image oldImage = campaign.getImage();
        campaign.setImage(newImage);
        campaignRepository.save(campaign);

        if (oldImage != null) {
            try {
                imageService.deleteImage(oldImage);
            } catch (IOException ignored) {
            }
        }

        return ResponseEntity.ok(Map.of("url", newImage.getImageUrl(), "publicId", newImage.getImageId()));
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> deleteCampaignImage(@PathVariable Long id) throws IOException {
        Campaign campaign = campaignService.getEntity(id);
        Image oldImage = campaign.getImage();
        if (oldImage != null) {
            campaign.setImage(null);
            campaignRepository.save(campaign);
            imageService.deleteImage(oldImage);
        }
        return ResponseEntity.noContent().build();
    }
}
