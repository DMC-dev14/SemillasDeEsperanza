package com.donaciones.Proyecto.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.donaciones.Proyecto.model.Donador;
import com.donaciones.Proyecto.repository.DonadorRepository;

@Service
public class DonadorService {

    @Autowired
    private DonadorRepository donadorRepository;

    @Autowired
    private EmailService emailService;

    public ResponseEntity<Donador> registrar(Donador donador) {
        // Validar que no exista el email
        if (donadorRepository.existsByEmail(donador.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        Donador guardado = donadorRepository.save(donador);
        return ResponseEntity.ok(guardado);
    }

    public List<Donador> listar() {
        return donadorRepository.findAll();
    }

    public ResponseEntity<Donador> login(String email, String contrasenia) {
        Optional<Donador> donadorOpt = donadorRepository.findByEmail(email);
        if (donadorOpt.isPresent()) {
            Donador donador = donadorOpt.get();
            // En un proyecto real, las contraseñas deben estar encriptadas.
            // Aquí comparamos en texto plano como en el código original.
            if (donador.getContrasenia().equals(contrasenia)) {
                return ResponseEntity.ok(donador);
            }
        }
        return ResponseEntity.status(401).build(); // 401 Unauthorized
    }

    public ResponseEntity<Donador> buscar(Long id) {
        return donadorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> generarTokenReset(String email) {

        Optional<Donador> donadorOpt = donadorRepository.findByEmail(email);

        if (donadorOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No existe una cuenta con ese correo."));
        }

        Donador donador = donadorOpt.get();

        Random random = new Random();
        String codigo = String.format("%06d", random.nextInt(1000000));

        donador.setCodigoRecuperacion(codigo);
        donador.setCodigoExpiracion(LocalDateTime.now().plusMinutes(10));

        try {
            emailService.enviarCorreoReset(donador.getEmail(), codigo);
            // Solo guardamos si el correo se envió con éxito
            donadorRepository.save(donador);
            return ResponseEntity.ok(
                    Map.of("mensaje", "Se envió un código de verificación a tu correo."));
        } catch (Exception e) {
            // Si el correo falla, no se guarda el código y se notifica el error.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo enviar el correo de recuperación. Inténtalo más tarde."));
        }
    }

    public ResponseEntity<?> restablecerContrasenia(String email,
            String codigo,
            String nuevaContrasenia) {

        Optional<Donador> donadorOpt = donadorRepository.findByEmailAndCodigoRecuperacionAndCodigoExpiracionAfter(
                email,
                codigo,
                LocalDateTime.now());

        if (donadorOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Código incorrecto o expirado."));
        }

        Donador donador = donadorOpt.get();

        donador.setContrasenia(nuevaContrasenia);

        donador.setCodigoRecuperacion(null);
        donador.setCodigoExpiracion(null);

        donadorRepository.save(donador);

        return ResponseEntity.ok(
                Map.of("mensaje", "Contraseña actualizada correctamente."));
    }

    public ResponseEntity<?> cambiarContrasenia(Long donadorId, String passActual, String passNueva) {
        Optional<Donador> donadorOpt = donadorRepository.findById(donadorId);

        if (donadorOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado."));
        }

        Donador donador = donadorOpt.get();

        // Aquí se debería usar un codificador de contraseñas (BCrypt) en un proyecto
        // real
        if (!donador.getContrasenia().equals(passActual)) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña actual es incorrecta."));
        }

        donador.setContrasenia(passNueva);
        donadorRepository.save(donador);

        return ResponseEntity.ok(Map.of("mensaje", "Tu contraseña ha sido actualizada con éxito."));
    }
}
