package com.donation.app.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donation.app.dto.request.LoginRequest;
import com.donation.app.dto.request.RegisterRequest;
import com.donation.app.dto.request.UpdateUserRequest;
import com.donation.app.dto.response.AuthResponse;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.exception.BusinessException;
import com.donation.app.repository.UserRepository;
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
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthResponse auth = authService.registerAndLogin(request);
        setJwtCookie(response, auth.token(), Duration.ofHours(24));
        return ResponseEntity.status(HttpStatus.CREATED).body(auth);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        setJwtCookie(response, auth.token(), Duration.ofHours(24));
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        setJwtCookie(response, "", Duration.ZERO);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return ResponseEntity.ok(UserService.mapToUserResponse(principal.getUser()));
    }

    @PatchMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @Valid @RequestBody UpdateUserRequest request,
                                                      HttpServletResponse response) {
        if (principal == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        UserResponse updated = userService.update(principal.getUser().getId(),
                new UpdateUserRequest(request.name(), request.email(), request.imageUrl(), null));
        var userEntity = userRepository.findById(updated.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        AuthResponse auth = authService.issueForUser(userEntity);
        setJwtCookie(response, auth.token(), Duration.ofHours(24));
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                                              @RequestBody Map<String, String> payload) {
        if (principal == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        userService.changePassword(principal.getUser().getId(), currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    private void setJwtCookie(HttpServletResponse response, String token, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}