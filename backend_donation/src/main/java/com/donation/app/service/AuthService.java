package com.donation.app.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donation.app.dto.request.ChangePasswordRequest;
import com.donation.app.dto.request.EmailRequest;
import com.donation.app.dto.request.ForgotPasswordRequest;
import com.donation.app.dto.request.LoginRequest;
import com.donation.app.dto.request.RegisterRequest;
import com.donation.app.dto.request.UpdateProfileRequest;
import com.donation.app.dto.response.AuthResponse;
import com.donation.app.dto.response.ResetPasswordResponse;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.User;
import com.donation.app.repository.UserRepository;
import com.donation.app.security.CustomUserDetailsService.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long RESET_TOKEN_MINUTES = 30;
    private static final String RESET_EMAIL_SUBJECT = "Recupera tu contraseña";

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByEmail(email.toLowerCase())
                        .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas")));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tu cuenta se encuentra inactiva. Contacta al administrador.");
        }

        return issueForUser(user);
    }

    public AuthResponse registerAndLogin(RegisterRequest request) {
        com.donation.app.dto.response.UserResponse userResp = userService.register(request);
        User savedUser = userRepository.findByEmail(userResp.email())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return issueForUser(savedUser);
    }

    /**
     * Actualiza el perfil del usuario autenticado y emite un token nuevo.
     * Es necesario un token nuevo porque el subject del JWT es el email: si el
     * usuario cambia su correo, el token anterior deja de ser válido.
     */
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserResponse updated = userService.updateOwnProfile(userId, request);
        User user = userRepository.findByEmail(updated.email())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return issueForUser(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        userService.changePassword(userId, request);
    }

    /**
     * Genera un token de recuperación de un solo uso y envía el enlace por correo.
     * Si el correo no existe o la cuenta está inactiva no se revela información:
     * el método termina sin hacer nada y el controller responde igualmente 200.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();
        if (!Boolean.TRUE.equals(user.getActive())) {
            return;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(token);
        user.setTokenExpiration(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES));
        userRepository.save(user);

        EmailRequest emailRequest = new EmailRequest(
                null, // el remitente se toma de la configuración en EmailService
                user.getEmail(),
                RESET_EMAIL_SUBJECT,
                user.getName(),
                token);

        emailService.sendEmailTemplate(emailRequest);
    }

    /**
     * Valida el token de recuperación, actualiza la contraseña y lo invalida
     * para que no pueda reutilizarse.
     */
    @Transactional
    public void resetPassword(ResetPasswordResponse request) {
        String token = request.token().trim();

        User user = userRepository
                .findByResetTokenAndTokenExpirationAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "El enlace de recuperación es inválido o ha expirado"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Tu cuenta se encuentra inactiva. Contacta al administrador.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe ser distinta a la actual");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setTokenExpiration(null);
        userRepository.save(user);
    }

    public AuthResponse issueForUser(User user) {
        JwtService.IssuedToken token = jwtService.issue(new CustomUserDetails(user));
        return new AuthResponse(
                token.value(),
                UserService.mapToUserResponse(user),
                token.expiresAt()
        );
    }
}
