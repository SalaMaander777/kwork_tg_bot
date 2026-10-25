package org.example.telegramkworkbot.admin.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.admin.AdminProperties;
import org.example.telegramkworkbot.admin.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record Me(boolean authenticated, boolean enabled, String username) {}

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final LoginAttemptService attempts;
    private final AdminProperties props;
    private final AuditService audit;

    /** Текущий пользователь. Заодно выдаёт cookie XSRF-TOKEN для последующих POST/PUT. */
    @GetMapping("/me")
    public Me me(Authentication authentication, CsrfToken csrf) {
        csrf.getToken();
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return new Me(authenticated, props.enabled(), authenticated ? authentication.getName() : null);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest body,
                                   HttpServletRequest request, HttpServletResponse response) {
        if (!props.enabled()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Админ-панель отключена: задайте ADMIN_USERNAME и ADMIN_PASSWORD_HASH");
        }
        String ip = request.getRemoteAddr();
        Duration blocked = attempts.blockedFor(ip);
        if (blocked != null) {
            return error(HttpStatus.TOO_MANY_REQUESTS,
                    "Слишком много попыток. Попробуйте через " + Math.max(1, blocked.toMinutes()) + " мин.");
        }
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
            attempts.succeeded(ip);

            // новая сессия после входа — защита от фиксации сессии
            if (request.getSession(false) != null) {
                request.changeSessionId();
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, request, response);

            audit.record("LOGIN", auth.getName(), null);
            return ResponseEntity.ok(new Me(true, true, auth.getName()));
        } catch (AuthenticationException e) {
            attempts.failed(ip);
            audit.record("LOGIN_FAILED", body.username(), null);
            return error(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
