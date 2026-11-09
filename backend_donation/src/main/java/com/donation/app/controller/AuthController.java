package com.donation.app.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.donation.app.dto.request.ChangePasswordRequest;
import com.donation.app.dto.request.ForgotPasswordRequest;
import com.donation.app.dto.request.LoginRequest;
import com.donation.app.dto.request.RegisterRequest;
import com.donation.app.dto.request.UpdateProfileRequest;
import com.donation.app.dto.response.AuthResponse;
import com.donation.app.dto.response.ResetPasswordResponse;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.User;
import com.donation.app.security.CustomUserDetailsService.CustomUserDetails;
import com.donation.app.security.JwtAuthenticationFilter;
import com.donation.app.service.AuthService;
import com.donation.app.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAndLogin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(UserService.mapToUserResponse(usuarioAutenticado(principal)));
    }

    @PatchMapping({"/me", "/profile"})
    public ResponseEntity<AuthResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = usuarioAutenticado(principal).getId();
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(usuarioAutenticado(principal).getId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Inicia el flujo de recuperación de contraseña. Responde 200 siempre, exista
     * o no el correo, para no revelar qué emails están registrados.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Restablece la contraseña usando el token de un solo uso enviado por correo.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordResponse request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * El JWT es stateless: no hay sesión que invalidar en el servidor. Se limpia
     * la cookie jwt_token (usada por las vistas Thymeleaf) y el cliente debe
     * descartar el token. En Fase 5 se puede añadir una blacklist si se requiere
     * invalidación real.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.JWT_COOKIE_NAME, "")
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadMyAvatar(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(userService.updateAvatar(usuarioAutenticado(principal).getId(), file));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<UserResponse> deleteMyAvatar(
            @AuthenticationPrincipal CustomUserDetails principal) throws IOException {
        return ResponseEntity.ok(userService.deleteAvatar(usuarioAutenticado(principal).getId()));
    }

    private User usuarioAutenticado(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return principal.getUser();
    }
}
