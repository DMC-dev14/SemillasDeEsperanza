package com.donation.app.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private void crearCampanasSiNoExisten(CampaignRepository campaignRepository) {
        if (campaignRepository.count() > 0) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();

        campaignRepository.save(Campaign.builder()
                .title("Aulas del Futuro")
                .description("Equipamos aulas modernas para 280 ninos en Villa El Salvador con tecnologia y material educativo.")
                .history("Construimos y equipamos aulas modernas para ninos en situacion de vulnerabilidad en Villa El Salvador.")
                .imageUrl("https://images.unsplash.com/photo-1641736047654-48c2546fcf19?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("45000.00"))
                .amountCollected(new BigDecimal("35100.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        campaignRepository.save(Campaign.builder()
                .title("Mesa Llena")
                .description("Almuerzo nutritivo diario para 420 ninos de escuelas publicas en Trujillo.")
                .history("Programa de nutricion escolar que garantiza un almuerzo balanceado diario a 420 ninos.")
                .imageUrl("https://images.unsplash.com/photo-1698795635937-31264cdbf359?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("32000.00"))
                .amountCollected(new BigDecimal("29120.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        campaignRepository.save(Campaign.builder()
                .title("Agua es Vida")
                .description("Sistemas de agua potable para 1,200 personas en comunidades remotas de Piura.")
                .history("Instalacion de sistemas de captacion y purificacion de agua en comunidades de Sechura.")
                .imageUrl("https://images.unsplash.com/photo-1698795635788-7291ee73e670?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("52000.00"))
                .amountCollected(new BigDecimal("28600.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        log.info("Campanas de ejemplo creadas.");
    }
}