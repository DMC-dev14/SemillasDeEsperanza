package com.donation.app.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.donation.app.dto.request.CreateUserRequest;
import com.donation.app.dto.request.RegisterRequest;
import com.donation.app.dto.request.UpdateUserRequest;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.enums.RolNombre;
import com.donation.app.exception.BusinessException;
import com.donation.app.model.Image;
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
    private final ImageService imageService;

    @Transactional // Escritura
    public UserResponse register(RegisterRequest request) {
<<<<<<< HEAD
        return crearUsuario(request.name(), request.email(), request.password(), RolNombre.USUARIO.name());
=======
        return mapToUserResponse(registerEntity(request));
    }

    @Transactional // Escritura
    public User registerEntity(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

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

        return userRepository.save(user);
    }

    @Transactional // Escritura
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = obtenerEntidad(userId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 100
                || !newPassword.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
>>>>>>> 7fb6b8b862f38f66996d35a75ac2a749c3382a17
    }

    @Transactional // Escritura
    public UserResponse createUser(CreateUserRequest request) {
        return crearUsuario(request.name(), request.email(), request.password(), request.rol());
    }

    /**
     * Punto único de creación de cuentas (tanto para el registro público como
     * para la creación desde el panel de administración).
     */
    private UserResponse crearUsuario(String name, String emailRaw, String password, String rolNombre) {
        String email = normalizarEmail(emailRaw);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        Rol rol = obtenerRol(rolNombre.trim());

        User user = new User();
        user.setName(name.trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
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
            String email = normalizarEmail(request.email());
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new BusinessException(HttpStatus.CONFLICT, "El email ya está registrado");
            }
            user.setEmail(email);
        }

        if (request.rol() != null && !request.rol().equals(nombreRolActual(user))) {
            // Un administrador no puede quitarse a sí mismo el rol (perdería el acceso).
            if (esUsuarioAutenticado(id)) {
                throw new BusinessException(HttpStatus.CONFLICT, "No puedes cambiar tu propio rol");
            }

            // Si es el último administrador activo, tampoco puede dejar de serlo.
            verificarNoEsUltimoAdmin(user);

            user.setRol(obtenerRol(request.rol().trim()));
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

    @Transactional
    public UserResponse updateAvatar(Long userId, MultipartFile file) throws IOException {
        User user = obtenerEntidad(userId);
        Image oldImage = user.getImage();

        Image newImage = imageService.uploadImage(file);
        user.setImage(newImage);
        User saved = userRepository.save(user);

        if (oldImage != null) {
            try {
                imageService.deleteImage(oldImage);
            } catch (IOException ex) {
                // Ignorar fallo al eliminar la imagen antigua
            }
        }

        return mapToUserResponse(saved);
    }

    @Transactional
    public UserResponse deleteAvatar(Long userId) throws IOException {
        User user = obtenerEntidad(userId);
        Image oldImage = user.getImage();

        if (oldImage == null) {
            return mapToUserResponse(user);
        }

        user.setImage(null);
        userRepository.save(user);

        imageService.deleteImage(oldImage);

        return mapToUserResponse(user);
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

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String nombreRolActual(User user) {
        return user.getRol() != null ? user.getRol().getName() : null;
    }

    private boolean esUsuarioAutenticado(Long id) {
        User actual = usuarioAutenticado();
        return actual != null && actual.getId().equals(id);
    }

    private void verificarNoEsPropiaCuenta(Long id) {
        if (esUsuarioAutenticado(id)) {
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
                    "No se puede desactivar, eliminar o cambiar el rol del último administrador");
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
            user.getImage() != null ? user.getImage().getImageUrl() : null,
            user.getActive(),
            user.getCreatedAccount(),
            user.getRol() != null ? user.getRol().getName() : null

        );
    }
}
