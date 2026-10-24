package com.donation.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AppApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void endToEndAuthRoleAndRouteProtectionFlow() throws Exception {
        String uniqueEmail = "nuevo.cliente." + System.currentTimeMillis() + "@semillas.org";

        // 1. Visitante sin sesión puede ver páginas públicas (/ y /donar), pero /dashboard y /admin redirigen a /login
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/donar"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // 2. Registro con contraseña débil falla con 400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Juan Perez\",\"email\":\"" + uniqueEmail + "\",\"password\":\"debil123\"}"))
                .andExpect(status().isBadRequest());

        // 3. Registro válido guarda en BD, inicia sesión automáticamente (token + cookie) con rol USUARIO
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Juan Perez\",\"email\":\"" + uniqueEmail + "\",\"password\":\"Segura123\"}"))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("jwt_token"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(uniqueEmail))
                .andExpect(jsonPath("$.user.rolName").value("USUARIO"))
                .andReturn();

        Cookie clientCookie = regResult.getResponse().getCookie("jwt_token");
        assertThat(clientCookie).isNotNull();

        // 4. Cliente con sesión puede acceder a /dashboard, pero si intenta entrar a /admin es redirigido a /dashboard
        mockMvc.perform(get("/dashboard").cookie(clientCookie))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin").cookie(clientCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // 5. Logout limpia la cookie de sesión
        mockMvc.perform(post("/api/auth/logout").cookie(clientCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("jwt_token", 0));

        // 6. Volver a iniciar sesión con el usuario recién registrado funciona contra la BD
        MvcResult reLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail + "\",\"password\":\"Segura123\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt_token"))
                .andExpect(jsonPath("$.user.email").value(uniqueEmail))
                .andReturn();
        assertThat(reLoginResult.getResponse().getCookie("jwt_token")).isNotNull();

        // 7. Login de Administrador y protección de rutas de Admin
        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@semillas.org\",\"password\":\"Admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.rolName").value("ADMINISTRADOR"))
                .andReturn();

        Cookie adminCookie = adminLogin.getResponse().getCookie("jwt_token");
        assertThat(adminCookie).isNotNull();

        String adminHtml = mockMvc.perform(get("/admin").cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(adminHtml).contains("Panel Administrador");
        assertThat(adminHtml).doesNotContain("Quiénes Somos");

        // Admin intentando entrar a /dashboard o /nosotros es redirigido a /admin
        mockMvc.perform(get("/dashboard").cookie(adminCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
        mockMvc.perform(get("/nosotros").cookie(adminCookie))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }
}
