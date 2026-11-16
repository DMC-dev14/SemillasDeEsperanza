package com.donation.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AppApplicationTests {

    private static final String ADMIN_EMAIL = "admin@semillas.org";
    private static final String ADMIN_PASSWORD = "Admin123";
    private static final String CLIENT_PASSWORD = "Segura123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void publicPagesAndEndpointsAreAccessibleWithoutToken() throws Exception {
        // Páginas públicas
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/donar")).andExpect(status().isOk());
        mockMvc.perform(get("/proyectos")).andExpect(status().isOk());

        // Páginas privadas redirigen al login
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // API pública de campañas
        MvcResult campaigns = mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();
        assertThat(campaigns.getResponse().getContentAsString()).isNotBlank();

        mockMvc.perform(get("/api/donations/public-feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topDonors").exists())
                .andExpect(jsonPath("$.recentFeed").exists());

        // API administrativa sigue protegida
        mockMvc.perform(get("/api/campaigns/admin")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/donations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousDonationIsAcceptedOnAnActiveCampaign() throws Exception {
        Long campaignId = createCampaign(adminToken(), "Campaña anónima " + System.nanoTime());

        mockMvc.perform(post("/api/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "campaignId": %d,
                                  "amount": 25.00,
                                  "donorName": "Donante Anónimo",
                                  "donorEmail": "anonimo@example.com",
                                  "paymentMethod": "yape",
                                  "donationType": "unica"
                                }
                                """.formatted(campaignId)))
                .andExpect(status().isCreated());
    }

    @Test
    void registerUpdateProfileAndChangePasswordFlow() throws Exception {
        String originalEmail = "nuevo.cliente." + System.nanoTime() + "@semillas.org";
        String updatedEmail = "actualizado." + System.nanoTime() + "@semillas.org";

        // Contraseña débil rechazada
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Juan Perez\",\"email\":\"" + originalEmail
                                + "\",\"password\":\"debil123\"}"))
                .andExpect(status().isBadRequest());

        // Registro válido: devuelve token y usuario
        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Juan Perez\",\"email\":\"" + originalEmail
                                + "\",\"password\":\"" + CLIENT_PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(originalEmail))
                .andExpect(jsonPath("$.user.rolName").value("USUARIO"))
                .andReturn();

        String token = JsonPath.read(register.getResponse().getContentAsString(), "$.token");

        // /me con Bearer
        mockMvc.perform(withToken(get("/api/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(originalEmail));

        // Actualizar perfil: devuelve token nuevo y el email actualizado
        MvcResult profile = mockMvc.perform(withToken(patch("/api/auth/profile"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Juan Pérez Actualizado\",\"email\":\"" + updatedEmail + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Juan Pérez Actualizado"))
                .andExpect(jsonPath("$.user.email").value(updatedEmail))
                .andReturn();

        String newToken = JsonPath.read(profile.getResponse().getContentAsString(), "$.token");

        // El token anterior ya no sirve porque el subject era el email viejo
        mockMvc.perform(withToken(get("/api/auth/me"), token))
                .andExpect(status().isUnauthorized());

        // El token nuevo sí funciona
        mockMvc.perform(withToken(get("/api/auth/me"), newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(updatedEmail));

        // No se puede tomar un email ya registrado
        mockMvc.perform(withToken(patch("/api/auth/me"), newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Juan Pérez\",\"email\":\"" + ADMIN_EMAIL + "\"}"))
                .andExpect(status().isConflict());

        // Contraseña actual incorrecta
        mockMvc.perform(withToken(post("/api/auth/change-password"), newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Incorrecta123\",\"newPassword\":\"NuevaClave123\"}"))
                .andExpect(status().isBadRequest());

        // Cambio de contraseña correcto
        mockMvc.perform(withToken(post("/api/auth/change-password"), newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + CLIENT_PASSWORD
                                + "\",\"newPassword\":\"NuevaClave123\"}"))
                .andExpect(status().isNoContent());

        // La contraseña anterior ya no funciona; la nueva sí
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + updatedEmail + "\",\"password\":\"" + CLIENT_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + updatedEmail + "\",\"password\":\"NuevaClave123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(updatedEmail));

        // Logout responde 204 y limpia la cookie jwt_token
        mockMvc.perform(withToken(post("/api/auth/logout"), newToken))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("jwt_token", 0));
    }

    @Test
    void adminEndpointsRequireAdministratorRole() throws Exception {
        String clientEmail = "cliente.rol." + System.nanoTime() + "@semillas.org";

        MvcResult clientRegister = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente Rol\",\"email\":\"" + clientEmail
                                + "\",\"password\":\"" + CLIENT_PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String clientToken = JsonPath.read(clientRegister.getResponse().getContentAsString(), "$.token");

        // Token válido con rol USUARIO: 403 en rutas de administrador
        mockMvc.perform(withToken(get("/api/users"), clientToken)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(get("/api/campaigns/admin"), clientToken)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(get("/api/donations"), clientToken)).andExpect(status().isForbidden());

        // Token válido con rol ADMINISTRADOR: 200
        String adminToken = adminToken();
        mockMvc.perform(withToken(get("/api/users"), adminToken)).andExpect(status().isOk());
        mockMvc.perform(withToken(get("/api/campaigns/admin"), adminToken)).andExpect(status().isOk());
        mockMvc.perform(withToken(get("/api/donations"), adminToken)).andExpect(status().isOk());

        // Admin en páginas de cliente es redirigido al panel
        mockMvc.perform(withToken(get("/dashboard"), adminToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        String adminHtml = mockMvc.perform(withToken(get("/admin"), adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(adminHtml).contains("Panel Administrador");
    }

    @Test
    void clientCanRemoveAvatarEndpointAndItRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/auth/me/avatar")).andExpect(status().isUnauthorized());
    }

    private String adminToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.rolName").value("ADMINISTRADOR"))
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.token");
    }

    private Long createCampaign(String token, String title) throws Exception {
        MvcResult created = mockMvc.perform(withToken(post("/api/campaigns"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Campaña creada por una prueba automatizada.",
                                  "targetAmount": 1000.00,
                                  "status": "ACTIVE"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
