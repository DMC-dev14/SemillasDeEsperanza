package com.donation.app.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donation.app.dto.request.CreateUserRequest;
import com.donation.app.dto.request.RegisterRequest;
import com.donation.app.dto.request.UpdateUserRequest;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.enums.RolNombre;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.Rol;
import com.donation.app.model.User;
import com.donation.app.repository.DonationRepository;
import com.donation.app.repository.RolRepository;
import com.donation.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Por defecto, todas las operaciones son de lectura
public class UserService {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final DonationRepository donationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional // Escritura
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        Rol rolUsuario = obtenerRol(RolNombre.USUARIO.name());

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRol(rolUsuario);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    @Transactional // Escritura
    public UserResponse createUser(CreateUserRequest request) {
        String email = request.email().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        Rol rol = obtenerRol(request.rol().trim());

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRol(rol);
        user.setActive(true);

        return mapToUserResponse(userRepository.save(user));
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserService::mapToUserResponse)
                .toList();
    }

    public UserResponse getById(Long id) {
        return mapToUserResponse(obtenerEntidad(id));
    }

    @Transactional // Escritura
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = obtenerEntidad(id);

        if (request.name() != null) {
            user.setName(request.name().trim());
        }

        if (request.email() != null) {
            String email = request.email().trim();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new BusinessException(HttpStatus.CONFLICT, "El email ya está registrado");
            }
            user.setEmail(email);
        }

        if (request.imageUrl() != null) {
            user.setImageUrl(request.imageUrl().trim());
        }

        if (request.active() != null && !request.active().equals(user.getActive())) {
            if (Boolean.FALSE.equals(request.active())) {
                verificarNoEsPropiaCuenta(id);
                verificarNoEsUltimoAdmin(user);
            }
            user.setActive(request.active());
        }

        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional // Escritura
    public void delete(Long id) {
        User user = obtenerEntidad(id);

        verificarNoEsPropiaCuenta(id);
        verificarNoEsUltimoAdmin(user);

        if (donationRepository.existsByDonor_Id(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "El usuario tiene donaciones registradas, no se puede eliminar");
        }

        userRepository.delete(user);
    }

    private User obtenerEntidad(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Rol obtenerRol(String nombre) {
        return rolRepository.findByName(nombre)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "El rol especificado no existe: " + nombre));
    }

    private void verificarNoEsPropiaCuenta(Long id) {
        User actual = usuarioAutenticado();

        if (actual != null && actual.getId().equals(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No puedes desactivar ni eliminar tu propia cuenta");
        }
    }

    private void verificarNoEsUltimoAdmin(User user) {
        boolean esAdminActivo = user.getRol() != null
                && RolNombre.ADMINISTRADOR.name().equals(user.getRol().getName())
                && Boolean.TRUE.equals(user.getActive());

        if (esAdminActivo
                && userRepository.countByRol_NameAndActiveTrue(RolNombre.ADMINISTRADOR.name()) <= 1) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No se puede desactivar ni eliminar al último administrador");
        }
    }

    private User usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    public static UserResponse mapToUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getImageUrl(),
            user.getActive(),
            user.getCreatedAccount(),
            user.getRol() != null ? user.getRol().getName() : null
        );
    }
}