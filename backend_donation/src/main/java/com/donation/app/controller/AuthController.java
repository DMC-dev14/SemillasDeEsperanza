package com.donation.app.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.donation.app.dto.request.LoginRequest;
import com.donation.app.dto.request.RegisterRequest;
import com.donation.app.dto.response.AuthResponse;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.exception.BusinessException;
import com.donation.app.security.CustomUserDetailsService.CustomUserDetails;
import com.donation.app.service.AuthService;
import com.donation.app.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        return ResponseEntity.ok(UserService.mapToUserResponse(principal.getUser()));
    }

    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadMyAvatar(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart("file") MultipartFile file) throws IOException {
        if (principal == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return ResponseEntity.ok(userService.updateAvatar(principal.getUser().getId(), file));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<UserResponse> deleteMyAvatar(
            @AuthenticationPrincipal CustomUserDetails principal) throws IOException {
        if (principal == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return ResponseEntity.ok(userService.deleteAvatar(principal.getUser().getId()));
    }
}
