package com.donation.app.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.donation.app.security.CustomUserDetailsService;
import com.donation.app.security.JwtAuthenticationFilter;
import com.donation.app.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    @SuppressWarnings("null")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            // IF_REQUIRED mantiene la sesión para las vistas Thymeleaf (form login).
            // La API REST funciona con JWT y no depende de la sesión.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/", "/index",
                        "/login", "/registro",
                        "/proyectos", "/proyectos/**",
                        "/nosotros", "/contacto", "/donar",
                        "/css/**", "/js/**", "/images/**", "/favicon.ico"
                ).permitAll()
                // Solo el registro y el login son públicos. /api/auth/me exige token.
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/users/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll())
            .exceptionHandling(ex -> ex
                // Para la API siempre devolvemos JSON (401/403), nunca una redirección al login.
                .authenticationEntryPoint((request, response, authException) -> {
                    if (esRutaApi(request)) {
                        escribirErrorApi(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                                "Debes enviar un token JWT válido en el header Authorization: Bearer <token>");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (esRutaApi(request)) {
                        escribirErrorApi(response, HttpStatus.FORBIDDEN, "Acceso denegado",
                                "No tienes permisos para realizar esta operación");
                    } else {
                        response.sendError(HttpStatus.FORBIDDEN.value(),
                                "No tienes permisos para realizar esta operación");
                    }
                }))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean esRutaApi(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private void escribirErrorApi(HttpServletResponse response, HttpStatus status, String title, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
