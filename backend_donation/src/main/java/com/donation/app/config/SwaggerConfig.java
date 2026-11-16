package com.donation.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Define la documentación OpenAPI expuesta por Springdoc.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Construye la especificación OpenAPI con los metadatos, el contacto y la licencia de la API.
     *
     * @return modelo OpenAPI personalizado
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Donation API")
                        .version("1.0")
                        .description("API for managing donations and payments")
                        .contact(new Contact().name("Joel").email("joel@example.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}