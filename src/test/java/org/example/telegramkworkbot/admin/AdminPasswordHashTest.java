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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Хэш из htpasswd ($2y$), по ошибке положенный в ADMIN_PASSWORD: входить надо паролем, а не строкой хэша. */
@WebMvcTest(controllers = {AuthController.class, DashboardController.class})
@Import({AdminSecurityConfig.class, LoginAttemptService.class})
@TestPropertySource(properties = {"admin.username=admin", "admin.password=$2y$10$.69kMb/xBV9RqsAy2DWgyuWZN/y31kBqRtCOO7FGH1ZagJIIOp/s2"})
class AdminPasswordHashTest {

    private static final String HASH = "$2y$10$.69kMb/xBV9RqsAy2DWgyuWZN/y31kBqRtCOO7FGH1ZagJIIOp/s2";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MetricsService metrics;

    @MockitoBean
    private AuditService audit;

    @Test
    void passwordWorksAndHashStringDoesNot() throws Exception {
        login("s3cret").andExpect(status().isOk());
        login(HASH).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
        return mvc.perform(post("/admin/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"" + password + "\"}"));
    }
}
