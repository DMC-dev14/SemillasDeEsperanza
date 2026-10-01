package com.donation.app.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.donation.app.dto.request.RegisterRequest;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.enums.RolNombre;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.Rol;
import com.donation.app.model.User;
import com.donation.app.repository.RolRepository;
import com.donation.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request){
        String email=request.email().trim();

        if (userRepository.existsByEmail(email)){
            throw new BusinessException(HttpStatus.CONFLICT, "El email ya esta registrado");
        }

        Rol rolUsuario=rolRepository.findByName(RolNombre.USUARIO.name())
        .orElseThrow(() -> new BusinessException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Rol USUARIO no encontrado"));

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRol(rolUsuario);
        user.setActive(true);

        User savedUser=userRepository.saveAndFlush(user);
        return mapToUserResponse(savedUser);
    }

    public static UserResponse mapToUserResponse(User user){
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getImageUrl(),
            user.getActive(),
            user.getCreatedAccount(),
            user.getRol() !=null ? user.getRol().getName() : null
        );
    }
}
