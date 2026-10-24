package com.donation.app.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.donation.app.dto.request.LoginRequest;
import com.donation.app.dto.request.RegisterRequest;
import com.donation.app.dto.response.AuthResponse;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.User;
import com.donation.app.repository.UserRepository;
import com.donation.app.security.CustomUserDetailsService.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        User savedUser = userService.registerEntity(request);
        return issueForUser(savedUser);
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
