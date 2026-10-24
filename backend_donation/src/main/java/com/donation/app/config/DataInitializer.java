package com.donation.app.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.donation.app.enums.CampaignStatus;
import com.donation.app.enums.RolNombre;
import com.donation.app.model.Campaign;
import com.donation.app.model.Image;
import com.donation.app.model.Rol;
import com.donation.app.model.User;
import com.donation.app.repository.CampaignRepository;
import com.donation.app.repository.RolRepository;
import com.donation.app.repository.UserRepository;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(
            RolRepository rolRepository,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {

        return args -> {
            asignarRolSiNoExiste(rolRepository, RolNombre.USUARIO.name());
            Rol rolAdmin = asignarRolSiNoExiste(rolRepository, RolNombre.ADMINISTRADOR.name());
            crearAdminSiNoExiste(userRepository, passwordEncoder, rolAdmin, adminEmail, adminPassword);
            crearCampanasSiNoExisten(campaignRepository);
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

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        Optional<User> existente = userRepository.findByEmail(emailNormalizado);

        if (existente.isPresent()) {
            // Si el correo configurado ya existe, garantizamos que sea un administrador activo.
            // No se sobrescribe la contraseña para no pisar cambios hechos por el usuario.
            User admin = existente.get();
            boolean cambios = false;

            if (admin.getRol() == null || !RolNombre.ADMINISTRADOR.name().equals(admin.getRol().getName())) {
                admin.setRol(rolAdmin);
                cambios = true;
                log.warn("El usuario {} existia sin rol ADMINISTRADOR; se corrigio el rol.", emailNormalizado);
            }

            if (!Boolean.TRUE.equals(admin.getActive())) {
                admin.setActive(true);
                cambios = true;
                log.warn("El usuario {} estaba inactivo; se activo.", emailNormalizado);
            }

            if (cambios) {
                userRepository.saveAndFlush(admin);
            }

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

    private void crearCampanasSiNoExisten(CampaignRepository campaignRepository) {
        if (campaignRepository.count() > 0) {
            return;
        }

        campaignRepository.save(campana(
                "Aulas del Futuro",
                "Equipamos aulas modernas para 280 niños en Villa El Salvador con tecnología y material educativo.",
                "Construimos y equipamos aulas modernas para niños en situación de vulnerabilidad. Incluye computadoras, libros y capacitación docente.",
                "https://images.unsplash.com/photo-1641736047654-48c2546fcf19?w=800&h=450&fit=crop&auto=format",
                new BigDecimal("45000.00"), new BigDecimal("35100.00")));

        campaignRepository.save(campana(
                "Mesa Llena",
                "Almuerzo nutritivo diario para 420 niños de escuelas públicas en Trujillo.",
                "Programa de nutrición escolar que garantiza un almuerzo balanceado diario a niños de tres escuelas públicas en El Porvenir.",
                "https://images.unsplash.com/photo-1698795635937-31264cdbf359?w=800&h=450&fit=crop&auto=format",
                new BigDecimal("32000.00"), new BigDecimal("29120.00")));

        campaignRepository.save(campana(
                "Techo y Esperanza",
                "Rehabilitamos viviendas en zonas de riesgo para familias con niños en Iquitos.",
                "Rehabilitamos viviendas precarias en las orillas del río Itaya. Cada casa recibe piso de concreto, techo seguro y agua potable.",
                "https://images.unsplash.com/photo-1698795635846-cd9049746761?w=800&h=450&fit=crop&auto=format",
                new BigDecimal("60000.00"), new BigDecimal("37200.00")));

        log.info("Se crearon campanas de ejemplo.");
    }

    private Campaign campana(String title, String description, String history, String imageUrl,
            BigDecimal targetAmount, BigDecimal amountCollected) {
        Image image = Image.builder()
                .nombre(title)
                .imageUrl(imageUrl)
                .build();

        return Campaign.builder()
                .title(title)
                .description(description)
                .history(history)
                .image(image)
                .targetAmount(targetAmount)
                .amountCollected(amountCollected)
                .status(CampaignStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .build();
    }
}