package com.donation.app.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
<<<<<<< HEAD
import java.util.Locale;
import java.util.Optional;
=======
import java.util.List;
>>>>>>> 7fb6b8b862f38f66996d35a75ac2a749c3382a17

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.donation.app.enums.CampaignStatus;
import com.donation.app.enums.Method;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.enums.RolNombre;
import com.donation.app.model.Campaign;
<<<<<<< HEAD
import com.donation.app.model.Image;
=======
import com.donation.app.model.Donation;
>>>>>>> 7fb6b8b862f38f66996d35a75ac2a749c3382a17
import com.donation.app.model.Rol;
import com.donation.app.model.Transaction;
import com.donation.app.model.User;
import com.donation.app.model.Voucher;
import com.donation.app.repository.CampaignRepository;
import com.donation.app.repository.DonationRepository;
import com.donation.app.repository.RolRepository;
import com.donation.app.repository.TransactionRepository;
import com.donation.app.repository.UserRepository;
import com.donation.app.repository.VoucherRepository;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(
            RolRepository rolRepository,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            DonationRepository donationRepository,
            TransactionRepository transactionRepository,
            VoucherRepository voucherRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {

        return args -> {
            Rol rolUsuario = asignarRolSiNoExiste(rolRepository, RolNombre.USUARIO.name());
            Rol rolAdmin = asignarRolSiNoExiste(rolRepository, RolNombre.ADMINISTRADOR.name());
            crearAdminSiNoExiste(userRepository, passwordEncoder, rolAdmin, adminEmail, adminPassword);
            User clienteDemo = crearClienteDemoSiNoExiste(userRepository, passwordEncoder, rolUsuario);
            crearCampanasSiNoExisten(campaignRepository);
            crearDonacionesInicialesSiNoExisten(
                    donationRepository, transactionRepository, voucherRepository, campaignRepository, clienteDemo);
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

<<<<<<< HEAD
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
=======
        String emailNormalizado = email.trim().toLowerCase();
>>>>>>> 7fb6b8b862f38f66996d35a75ac2a749c3382a17

            return;
        }

        User admin = new User();
        admin.setName("Valentina Ríos (Admin)");
        admin.setEmail(emailNormalizado);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRol(rolAdmin);
        admin.setActive(true);
        userRepository.saveAndFlush(admin);

        log.info("Usuario administrador creado con email: {}", emailNormalizado);
    }

    private User crearClienteDemoSiNoExiste(UserRepository userRepository, PasswordEncoder passwordEncoder, Rol rolUsuario) {
        String emailDemo = "cliente@semillas.org";
        return userRepository.findByEmail(emailDemo).orElseGet(() -> {
            User cliente = new User();
            cliente.setName("María García");
            cliente.setEmail(emailDemo);
            cliente.setPassword(passwordEncoder.encode("Cliente123"));
            cliente.setRol(rolUsuario);
            cliente.setActive(true);
            User saved = userRepository.saveAndFlush(cliente);
            log.info("Usuario cliente de prueba creado con email: {}", emailDemo);
            return saved;
        });
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

<<<<<<< HEAD
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
=======
        campaignRepository.save(Campaign.builder()
                .title("Aulas del Futuro")
                .description("Equipamos aulas modernas para 280 niños en Villa El Salvador con tecnología y material educativo.")
                .history("Construimos y equipamos aulas modernas para niños en situación de vulnerabilidad en Villa El Salvador.")
                .imageUrl("https://images.unsplash.com/photo-1641736047654-48c2546fcf19?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("45000.00"))
                .amountCollected(new BigDecimal("35100.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        campaignRepository.save(Campaign.builder()
                .title("Mesa Llena")
                .description("Almuerzo nutritivo diario para 420 niños de escuelas públicas en Trujillo.")
                .history("Programa de nutrición escolar que garantiza un almuerzo balanceado diario a 420 niños en Trujillo.")
                .imageUrl("https://images.unsplash.com/photo-1698795635937-31264cdbf359?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("32000.00"))
                .amountCollected(new BigDecimal("29120.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        campaignRepository.save(Campaign.builder()
                .title("Agua es Vida")
                .description("Sistemas de agua potable para 1,200 personas en comunidades remotas de Piura.")
                .history("Instalación de sistemas de captación y purificación de agua en comunidades de Sechura, Piura.")
                .imageUrl("https://images.unsplash.com/photo-1698795635788-7291ee73e670?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("52000.00"))
                .amountCollected(new BigDecimal("28600.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        campaignRepository.save(Campaign.builder()
                .title("Techo y Esperanza")
                .description("Rehabilitamos viviendas en zonas de riesgo para familias con niños en Iquitos.")
                .history("Rehabilitamos viviendas precarias en Belén, Iquitos con piso firme, techo seguro y agua potable.")
                .imageUrl("https://images.unsplash.com/photo-1698795635846-cd9049746761?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("60000.00"))
                .amountCollected(new BigDecimal("37200.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        campaignRepository.save(Campaign.builder()
                .title("Sonrisas Saludables")
                .description("Brigadas de salud mensuales para niños en comunidades rurales de Cusco sin atención médica.")
                .history("Brigadas médicas mensuales que llevan atención pediátrica y odontológica a comunidades rurales de Cusco.")
                .imageUrl("https://images.unsplash.com/photo-1698795635788-7291ee73e670?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("28000.00"))
                .amountCollected(new BigDecimal("12640.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        campaignRepository.save(Campaign.builder()
                .title("Talentos sin Límites")
                .description("Talleres artísticos y deportivos para adolescentes en riesgo de deserción escolar en Lima.")
                .history("Talleres de arte, música y deporte para adolescentes en Villa El Salvador y San Juan de Lurigancho.")
                .imageUrl("https://images.unsplash.com/photo-1700936206562-8c4cdb1d1926?w=800&h=450&fit=crop&auto=format")
                .targetAmount(new BigDecimal("18500.00"))
                .amountCollected(new BigDecimal("6105.00"))
                .status(CampaignStatus.ACTIVE)
                .startDate(ahora)
                .build());

        log.info("Campañas iniciales creadas.");
    }

    private void crearDonacionesInicialesSiNoExisten(
            DonationRepository donationRepository,
            TransactionRepository transactionRepository,
            VoucherRepository voucherRepository,
            CampaignRepository campaignRepository,
            User clienteDemo) {

        if (donationRepository.count() > 0) {
            return;
        }

        List<Campaign> campaigns = campaignRepository.findAll();
        if (campaigns.isEmpty() || clienteDemo == null) {
            return;
        }

        Campaign c1 = campaigns.get(0);
        Campaign c2 = campaigns.size() > 1 ? campaigns.get(1) : c1;
        Campaign c3 = campaigns.size() > 2 ? campaigns.get(2) : c1;

        registrarDonacionSemilla(donationRepository, transactionRepository, voucherRepository,
                clienteDemo, c1, new BigDecimal("150.00"), Method.CREDIT_CARD, "Apoyo para equipamiento escolar - María García");
        registrarDonacionSemilla(donationRepository, transactionRepository, voucherRepository,
                clienteDemo, c2, new BigDecimal("100.00"), Method.YAPE, "Donación para comedores escolares - María García");
        registrarDonacionSemilla(donationRepository, transactionRepository, voucherRepository,
                clienteDemo, c3, new BigDecimal("200.00"), Method.BANK_TRANSFER, "Agua limpia para las familias - María García");

        log.info("Donaciones iniciales creadas en la base de datos.");
    }

    private void registrarDonacionSemilla(
            DonationRepository donationRepository,
            TransactionRepository transactionRepository,
            VoucherRepository voucherRepository,
            User donor,
            Campaign campaign,
            BigDecimal amount,
            Method method,
            String message) {

        Donation d = Donation.builder()
                .amount(amount)
                .currency("PEN")
                .message(message)
                .status(PaymentStatus.COMPLETED)
                .donor(donor)
                .campaign(campaign)
                .build();
        Donation saved = donationRepository.save(d);

        Transaction tx = Transaction.builder()
                .codeReferential("TX-SEED-00" + saved.getId())
                .methodPay(method)
                .amount(amount)
                .currency("PEN")
                .status(PaymentStatus.COMPLETED)
                .donation(saved)
                .build();
        Transaction savedTx = transactionRepository.save(tx);

        Voucher v = new Voucher();
        v.setNumber(String.format("REC-2026-%05d", saved.getId()));
        v.setEmailSend(donor.getEmail());
        v.setFilePath("/api/donations/" + saved.getId());
        v.setTransaction(savedTx);
        voucherRepository.save(v);
>>>>>>> 7fb6b8b862f38f66996d35a75ac2a749c3382a17
    }
}