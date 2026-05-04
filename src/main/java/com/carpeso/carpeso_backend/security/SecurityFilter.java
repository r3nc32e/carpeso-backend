package com.carpeso.carpeso_backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class SecurityFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest)  request;
        HttpServletResponse res = (HttpServletResponse) response;

        // ── Security Headers (OWASP recommended) ──────────────────────────
        res.setHeader("X-Content-Type-Options",  "nosniff");
        res.setHeader("X-Frame-Options",          "DENY");
        res.setHeader("X-XSS-Protection",         "1; mode=block");
        res.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");
        res.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");
        res.setHeader("Referrer-Policy",          "strict-origin-when-cross-origin");
        res.setHeader("Permissions-Policy",       "geolocation=(), microphone=(), camera=()");

        // ── Carpeso Security Stack identifier (visible in DevTools) ───────
        res.setHeader("X-Security-Stack",
                "JWT + RBAC + OTP + AES-256 + RateLimit + InputValidation");

        chain.doFilter(request, response);
    }
}