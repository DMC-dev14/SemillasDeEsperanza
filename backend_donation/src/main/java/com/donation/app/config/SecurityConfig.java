package com.donation.app.config;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.donation.app.security.CustomUserDetailsService;
import com.donation.app.security.JwtAuthenticationFilter;
import com.donation.app.service.JwtService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    @SuppressWarnings("null")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/campaigns", "/api/campaigns/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/donations").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/donations/public-feed").permitAll()
                .requestMatchers("/dashboard", "/dashboard/**").hasRole("USUARIO")
                .requestMatchers("/admin", "/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/users/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"No autenticado. Inicia sesión para continuar.\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"title\":\"Forbidden\",\"status\":403,\"detail\":\"No tienes permisos para acceder a este recurso.\"}");
                    } else {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null && auth.getAuthorities().stream()
                                .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()))) {
                            response.sendRedirect("/admin");
                        } else if (auth != null && auth.getAuthorities().stream()
                                .anyMatch(a -> "ROLE_USUARIO".equals(a.getAuthority()))) {
                            response.sendRedirect("/dashboard");
                        } else {
                            response.sendRedirect("/login");
                        }
                    }
                }))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}