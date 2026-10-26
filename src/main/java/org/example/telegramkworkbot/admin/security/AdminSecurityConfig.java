package org.example.telegramkworkbot.admin.security;

import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.admin.AdminProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.regex.Pattern;

/**
 * Доступ в админ-панель: один администратор из настроек, сессия + CSRF (cookie XSRF-TOKEN для SPA).
 * API — /admin/api/**, статика SPA — /admin/**, всё остальное закрыто.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminSecurityConfig {

    private static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService adminUsers(AdminProperties props, PasswordEncoder encoder) {
        if (!props.enabled()) {
            log.warn("Админ-панель отключена: задайте ADMIN_USERNAME и ADMIN_PASSWORD_HASH (или ADMIN_PASSWORD)");
            return username -> {
                throw new UsernameNotFoundException("Админ-панель отключена");
            };
        }
        String hash;
        if (props.passwordHash() != null && !props.passwordHash().isBlank()) {
            hash = props.passwordHash().strip();
        } else if (BCRYPT.matcher(props.password().strip()).matches()) {
            // хэш по ошибке положили в ADMIN_PASSWORD — иначе паролем стала бы сама строка хэша
            log.warn("ADMIN_PASSWORD похож на bcrypt-хэш — используем его как хэш. Переименуйте переменную в ADMIN_PASSWORD_HASH");
            hash = props.password().strip();
        } else {
            hash = encoder.encode(props.password());
        }
        log.info("Админ-панель включена для пользователя «{}»", props.username());
        return new InMemoryUserDetailsManager(User.withUsername(props.username()).password(hash).roles("ADMIN").build());
    }

    @Bean
    public AuthenticationManager adminAuthenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http, SecurityContextRepository contextRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/api/auth/**").permitAll()
                        .requestMatchers("/admin/api/**").hasRole("ADMIN")
                        .requestMatchers("/", "/admin", "/admin/**", "/error").permitAll()
                        .anyRequest().denyAll())
                .csrf(csrf -> csrf.spa())
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/admin/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                                        + "script-src 'self'; connect-src 'self'; frame-ancestors 'none'")));
        return http.build();
    }
}
