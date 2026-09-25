package com.donation.app.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.donation.app.model.Rol;

import com.donation.app.repository.RolRepository;

@Configuration 
public class DataInitializer {
    @Bean
    public CommandLineRunner initData(RolRepository rolRepository){
        return args ->{
            asignarRolSiNoExiste (rolRepository, "USUARIO");
            asignarRolSiNoExiste(rolRepository, "ADMINISTRADOR");
        };
    }

    private void asignarRolSiNoExiste(RolRepository rolRepository, String nombreRol) {
    if (rolRepository.findByName(nombreRol).isEmpty()) {
        Rol rol = new Rol();
        rol.setName(nombreRol);
        rolRepository.save(rol);
        System.out.println("Rol creado exitosamente: " + nombreRol);
    }
}
}

