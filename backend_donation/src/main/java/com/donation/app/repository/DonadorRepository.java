package com.donaciones.Proyecto.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donaciones.Proyecto.model.Donador;

@Repository
public interface DonadorRepository extends JpaRepository<Donador, Long> {

    Optional<Donador> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Donador> findByNombreContainingIgnoreCase(String nombre);

    Optional<Donador> findByEmailAndCodigoRecuperacionAndCodigoExpiracionAfter(
            String email,
            String codigo,
            LocalDateTime now);
}
