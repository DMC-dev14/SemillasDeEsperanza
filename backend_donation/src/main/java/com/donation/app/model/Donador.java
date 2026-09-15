package com.donaciones.Proyecto.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "donadores")
public class Donador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 20)
    private String contrasenia;

    @Column(length = 6)
    private String codigoRecuperacion;

    private LocalDateTime codigoExpiracion;

    @Column(nullable = false, length = 20)
    private String rol = "USER"; // Valor por defecto "USER"

    // --- CONSTRUCTORES NATIVOS ---
    // Constructor vacío requerido por JPA/Hibernate
    public Donador() {
    }

    // Constructor completo (equivalente al @AllArgsConstructor de Lombok)
    public Donador(Long id, String nombre, String email, String contrasenia, String codigoRecuperacion,
            LocalDateTime codigoExpiracion, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.contrasenia = contrasenia;
        this.codigoRecuperacion = codigoRecuperacion;
        this.codigoExpiracion = codigoExpiracion;
        this.rol = rol;
    }

    // --- GETTERS Y SETTERS NATIVOS (Equivalentes al @Data de Lombok) ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    public String getCodigoRecuperacion() {
        return codigoRecuperacion;
    }

    public void setCodigoRecuperacion(String codigoRecuperacion) {
        this.codigoRecuperacion = codigoRecuperacion;
    }

    public LocalDateTime getCodigoExpiracion() {
        return codigoExpiracion;
    }

    public void setCodigoExpiracion(LocalDateTime codigoExpiracion) {
        this.codigoExpiracion = codigoExpiracion;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
