package com.donation.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Registra el codificador de contraseñas basado en BCrypt usado en toda la aplicación.
 */
@Configuration 
public class PasswordConfig {
    /**
     * Proporciona el {@link PasswordEncoder} BCrypt para cifrar y verificar contraseñas.
     *
     * @return codificador BCrypt
     */
    @Bean 
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
