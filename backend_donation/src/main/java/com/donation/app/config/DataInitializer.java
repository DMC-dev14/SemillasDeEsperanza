package com.donation.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.donation.app.enums.RolNombre;
import com.donation.app.model.Rol;
import com.donation.app.model.User;
import com.donation.app.repository.RolRepository;
import com.donation.app.repository.UserRepository;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(
            RolRepository rolRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {

        return args -> {
            asignarRolSiNoExiste(rolRepository, RolNombre.USUARIO.name());
            Rol rolAdmin = asignarRolSiNoExiste(rolRepository, RolNombre.ADMINISTRADOR.name());
            crearAdminSiNoExiste(userRepository, passwordEncoder, rolAdmin, adminEmail, adminPassword);
        };
    }

    private Rol asignarRolSiNoExiste(RolRepository rolRepository, String nombreRol) {
        return rolRepository.findByName(nombreRol).orElseGet(() -> {
            Rol rol = new Rol();
            rol.setName(nombreRol);
            Rol saved = rolRepository.save(rol);
            log.info("Rol creado exitosamente: {}", nombreRol);
            return saved;
        });
    }

    private void crearAdminSiNoExiste(UserRepository userRepository, PasswordEncoder passwordEncoder,
            Rol rolAdmin, String email, String password) {

        String emailNormalizado = email.trim();

        if (userRepository.existsByEmail(emailNormalizado)) {
            return;
        }

        User admin = new User();
        admin.setName("Administrador");
        admin.setEmail(emailNormalizado);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRol(rolAdmin);
        admin.setActive(true);
        userRepository.saveAndFlush(admin);

        log.info("Usuario administrador creado con email: {}", emailNormalizado);
        log.warn("Revisa app.admin.password: el administrador se creo con la contrasena de configuracion.");
    }
}