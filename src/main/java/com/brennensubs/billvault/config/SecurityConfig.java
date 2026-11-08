package com.brennensubs.billvault.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  UserDetailsService users(
      @Value("${billvault.admin.username}") String username,
      @Value("${billvault.admin.password}") String password) {
    if (username.isBlank()) throw new IllegalArgumentException("ADMIN_USERNAME must not be blank");
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "ADMIN_PASSWORD must be at most 72 UTF-8 bytes for BCrypt");
    if (password.startsWith("replace-with-"))
      throw new IllegalArgumentException(
          "Replace the example administrator password before startup");
    if (password.length() < 12)
      throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 12 characters");
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password))
            .roles("ADMIN")
            .build());
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(a ->
            a.requestMatchers(
                    "/api/auth/csrf",
                    "/api/auth/login",
                    "/actuator/health",
                    "/",
                    "/index.html",
                    "/assets/**",
                    "/favicon.svg")
                .permitAll()
                .anyRequest()
                .authenticated())
        .formLogin(f ->
            f.loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> res.setStatus(204))
                .failureHandler((req, res, ex) -> problem(res, 401, "Invalid credentials")))
        .logout(l ->
            l.logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .exceptionHandling(e ->
            e.authenticationEntryPoint(
                    (req, res, ex) -> problem(res, 401, "Authentication required"))
                .accessDeniedHandler((req, res, ex) ->
                    problem(res, 403, "Access denied or invalid security token. Sign in again.")))
        .headers(h ->
            h.contentSecurityPolicy(c ->
                c.policyDirectives(
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';"
                        + " img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src"
                        + " 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
        .build();
  }

  private static void problem(HttpServletResponse response, int status, String detail)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response
        .getWriter()
        .write(
            "{\"status\":"
                + status
                + ",\"title\":\"Request rejected\",\"detail\":\""
                + detail
                + "\"}");
  }
}
