package com.donation.app.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.donation.app.dto.request.LoginRequest;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Usuario inactivo");
        }

        JwtService.IssuedToken token = jwtService.issue(new CustomUserDetails(user));

        return new AuthResponse(
                token.value(),
                UserService.mapToUserResponse(user),
                token.expiresAt()
        );
    }
}
