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

/**
 * Configuración central de Spring Security: reglas de autorización, autenticación con JWT, login
 * por formulario para las vistas Thymeleaf y manejo de errores de la API.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    /**
     * Define la cadena de filtros de seguridad. La sesión se mantiene para el login de las vistas
     * Thymeleaf, mientras que la API REST funciona con JWT. Las donaciones y los endpoints públicos
     * (campañas activas, feed público, registro/login y webhooks de pago) son accesibles sin
     * autenticación; el checkout de PayPal queda protegido por {@code checkoutToken}. Para las rutas
     * {@code /api/**} los errores de autenticación y autorización se devuelven como JSON (401/403)
     * en lugar de redirigir al login.
     *
     * @param http configuración de seguridad HTTP de Spring
     * @return la cadena de filtros de seguridad construida
     * @throws Exception si falla la construcción de la cadena de filtros
     */
    @Bean
    @SuppressWarnings("null")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/index",
                                "/login", "/registro",
                                "/proyectos", "/proyectos/**",
                                "/nosotros", "/contacto", "/donar",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/campaigns/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/campaigns/{id:\\d+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/campaigns/admin").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/campaigns", "/api/campaigns/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/donations/public-feed").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/donations").permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/payment-gateway/charge-payment",
                                "/api/payment-gateway/yape/token")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register", "/api/auth/login", "/api/auth/logout",
                                "/api/auth/forgot-password", "/api/auth/reset-password")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/payment-gateway/webhook")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/paypal/orders",
                                "/api/paypal/orders/*/capture",
                                "/api/paypal/webhook")
                        .permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/me/**",
                                "/api/auth/profile", "/api/auth/change-password")
                        .authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
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
