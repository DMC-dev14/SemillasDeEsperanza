package com.donation.app.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.donation.app.dto.request.LoginRequest;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.User;
import com.donation.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse login(LoginRequest request){
        String email=request.email().trim();

        User user=userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        if(!Boolean.TRUE.equals(user.getActive())){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Usuario inactivo");
        }

        return UserService.mapToUserResponse(user);
    }
}
