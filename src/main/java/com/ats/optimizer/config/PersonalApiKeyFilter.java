package com.ats.optimizer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Small, deliberate protection for this private single-user API. Local
 * development keeps it disabled; cloud deployment must enable it and supply a
 * secret through PERSONA_API_KEY.
 */
@Component
public class PersonalApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.security.require-api-key:false}")
    private boolean required;

    @Value("${app.security.api-key:}")
    private String configuredKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!required) {
            filterChain.doFilter(request, response);
            return;
        }

        if (configuredKey == null || configuredKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "API key protection is enabled but not configured");
            return;
        }

        String suppliedKey = request.getHeader("X-Persona-Api-Key");
        boolean valid = suppliedKey != null && MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid API key");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
