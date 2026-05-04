package com.carpeso.carpeso_backend.security;

import com.carpeso.carpeso_backend.config.VpnConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * VPN Simulation Filter — Carpeso Security Stack
 *
 * Enforces IP-based access control on admin and superadmin endpoints,
 * simulating a VPN tunnel at the application layer.
 *
 * How it works:
 *   1. Checks if the requested path is a VPN-protected endpoint
 *   2. Extracts the real client IP (proxy-aware)
 *   3. Checks if IP is in the whitelist (VpnConfig.allowedIps)
 *   4. If not whitelisted → 403 Forbidden with VPN rejection message
 *   5. Adds X-VPN-Status header to all responses (visible in DevTools)
 *
 * Order(2) = runs after RateLimitFilter(1) but before JwtFilter
 *
 * STRIDE mapping:
 *   - Spoofing: IP whitelist prevents unauthorized access even with stolen JWT
 *   - Information Disclosure: Admin data unreachable from non-VPN IPs
 *   - Elevation of Privilege: Attacker cannot reach admin endpoints from internet
 */
@Component
@Order(2)
public class VpnFilter implements Filter {

    @Autowired
    private VpnConfig vpnConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest)  request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path     = req.getRequestURI();
        String clientIp = extractIp(req);

        boolean isProtected = vpnConfig.isEnabled() &&
                vpnConfig.getProtectedPaths().stream()
                        .anyMatch(path::startsWith);

        if (isProtected) {
            boolean isAllowed = vpnConfig.getAllowedIps().stream()
                    .anyMatch(allowed -> allowed.equalsIgnoreCase(clientIp));

            // Add VPN status header — visible in browser DevTools Network tab
            if (isAllowed) {
                res.setHeader("X-VPN-Status",     "ALLOWED");
                res.setHeader("X-VPN-Client-IP",  clientIp);
                res.setHeader("X-VPN-Tunnel",     "IP-Whitelist-Active");
            } else {
                // Block access — 403 Forbidden
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json");
                res.setHeader("X-VPN-Status",    "BLOCKED");
                res.setHeader("X-VPN-Client-IP", clientIp);
                res.setHeader("X-VPN-Tunnel",    "IP-Whitelist-Active");

                Map<String, Object> body = Map.of(
                        "success",   false,
                        "message",   "Access denied. This endpoint is protected by VPN policy. " +
                                "Your IP (" + clientIp + ") is not in the trusted network.",
                        "ipAddress", clientIp,
                        "path",      path,
                        "timestamp", LocalDateTime.now().toString(),
                        "remedy",    "Connect through the Carpeso VPN tunnel to access admin resources."
                );
                objectMapper.writeValue(res.getWriter(), body);
                return;
            }
        } else {
            // Non-protected endpoint — mark as public in headers
            res.setHeader("X-VPN-Status", "PUBLIC-ENDPOINT");
        }

        chain.doFilter(request, response);
    }

    private String extractIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
            return forwarded.split(",")[0].trim();
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank())
            return realIp.trim();
        return req.getRemoteAddr();
    }
}