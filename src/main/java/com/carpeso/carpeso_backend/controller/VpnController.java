package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.config.VpnConfig;
import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * VPN Info Controller — Carpeso Security Stack
 *
 * Public endpoint that explains the VPN implementation.
 * Great for demonstrating to the professor.
 *
 * GET  /api/public/security/vpn-info  → shows full VPN config
 * POST /api/public/security/vpn-test  → tests if your IP is whitelisted
 */
@RestController
@RequestMapping("/api/public/security")
@CrossOrigin(origins = "*")
public class VpnController {

    @Autowired
    private VpnConfig vpnConfig;

    /**
     * GET /api/public/security/vpn-info
     * Returns the full VPN/IP whitelist configuration.
     * Shows the professor exactly how VPN simulation works.
     */
    @GetMapping("/vpn-info")
    public ResponseEntity<?> getVpnInfo(HttpServletRequest request) {
        String clientIp = extractIp(request);
        boolean isWhitelisted = vpnConfig.getAllowedIps().stream()
                .anyMatch(ip -> ip.equalsIgnoreCase(clientIp));

        Map<String, Object> info = Map.of(
                "vpnImplementation", Map.of(
                        "type",        "Application-Layer IP Whitelist (VPN Simulation)",
                        "description", vpnConfig.getTunnelDescription(),
                        "standard",    "Simulates WireGuard / OpenVPN tunnel access control",
                        "strideMapping", Map.of(
                                "spoofing",             "IP whitelist prevents access even with stolen JWT tokens",
                                "informationDisclosure","Admin data unreachable from non-whitelisted IPs",
                                "elevationOfPrivilege", "Admin endpoints inaccessible from public internet"
                        )
                ),
                "configuration", Map.of(
                        "enabled",        vpnConfig.isEnabled(),
                        "allowedIps",     vpnConfig.getAllowedIps(),
                        "protectedPaths", vpnConfig.getProtectedPaths(),
                        "enforcement",    "Filter-level (Order=2, runs before JWT validation)"
                ),
                "yourConnection", Map.of(
                        "ipAddress",   clientIp,
                        "status",      isWhitelisted ? "TRUSTED — You are inside the VPN tunnel" :
                                "UNTRUSTED — Your IP is not whitelisted",
                        "canAccessAdmin", isWhitelisted
                ),
                "productionNotes", Map.of(
                        "deployment",   "Replace allowedIps with actual VPN subnet (e.g., 10.8.0.0/24)",
                        "vpnGateway",   "WireGuard or OpenVPN server assigns IPs from trusted subnet",
                        "networkLayer", "Production: enforce at firewall/load balancer level as well"
                )
        );

        return ResponseEntity.ok(ApiResponse.success("VPN configuration", info));
    }

    /**
     * POST /api/public/security/vpn-test
     * Tests whether the caller's IP is whitelisted.
     * Demo-friendly — call from Postman or browser to show VPN in action.
     */
    @PostMapping("/vpn-test")
    public ResponseEntity<?> testVpnAccess(HttpServletRequest request) {
        String clientIp = extractIp(request);
        boolean allowed = vpnConfig.getAllowedIps().stream()
                .anyMatch(ip -> ip.equalsIgnoreCase(clientIp));

        if (allowed) {
            return ResponseEntity.ok(ApiResponse.success(
                    "✅ VPN ACCESS GRANTED — IP " + clientIp + " is in the trusted whitelist.",
                    Map.of("ip", clientIp, "status", "ALLOWED", "tunnel", "ACTIVE")
            ));
        } else {
            return ResponseEntity.status(403).body(ApiResponse.error(
                    "🚫 VPN ACCESS DENIED — IP " + clientIp +
                            " is not whitelisted. Connect through VPN tunnel first."
            ));
        }
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