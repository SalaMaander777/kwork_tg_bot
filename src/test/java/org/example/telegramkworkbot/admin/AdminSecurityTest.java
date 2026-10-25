package org.example.telegramkworkbot.admin;

import org.example.telegramkworkbot.admin.api.DashboardController;
import org.example.telegramkworkbot.admin.api.MetricsService;
import org.example.telegramkworkbot.admin.security.AdminSecurityConfig;
import org.example.telegramkworkbot.admin.security.AuthController;
import org.example.telegramkworkbot.admin.security.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, DashboardController.class})
@Import({AdminSecurityConfig.class, LoginAttemptService.class})
@TestPropertySource(properties = {"admin.username=admin", "admin.password=s3cret"})
class AdminSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MetricsService metrics;

    @MockitoBean
    private AuditService audit;

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/admin/api/dashboard/health")).andExpect(status().isUnauthorized());
    }

    @Test
    void otherPathsAreClosed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/admin/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"s3cret\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/admin/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Неверный логин или пароль"));
    }

    @Test
    void loginOpensSessionForApi() throws Exception {
        when(metrics.health()).thenReturn(List.of());
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/admin/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"s3cret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/admin/api/dashboard/health").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin/api/auth/me").session(session)).andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void bruteForceIsBlocked() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/admin/api/auth/login").with(csrf()).with(r -> { r.setRemoteAddr("9.9.9.9"); return r; })
                    .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"x\"}"));
        }
        mvc.perform(post("/admin/api/auth/login").with(csrf()).with(r -> { r.setRemoteAddr("9.9.9.9"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"s3cret\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
