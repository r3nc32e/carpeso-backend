package com.carpeso.carpeso_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * VPN / IP Whitelist Configuration — Carpeso Security Stack
 *
 * Simulates VPN tunnel access control by restricting admin-level endpoints
 * to a whitelist of trusted IP addresses.
 *
 * In a real production environment, this would be enforced at the
 * network layer via an actual VPN gateway (e.g., WireGuard, OpenVPN).
 * This implementation simulates that behavior at the application layer.
 *
 * Configured via application.properties:
 *   carpeso.vpn.enabled=true
 *   carpeso.vpn.allowed-ips=127.0.0.1,0:0:0:0:0:0:0:1
 */
@Configuration
@ConfigurationProperties(prefix = "carpeso.vpn")
public class VpnConfig {

    // Whether VPN/IP whitelist enforcement is active
    private boolean enabled = true;

    // List of allowed IPs — defaults to localhost (for demo)
    // In production: replace with actual office/VPN tunnel IPs
    private List<String> allowedIps = List.of(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",  // IPv6 localhost
            "::1"               // IPv6 loopback shorthand
    );

    // Endpoints that require VPN (IP whitelist) access
    // All /api/admin/** and /api/superadmin/** are VPN-protected by default
    private List<String> protectedPaths = List.of(
            "/api/admin/",
            "/api/superadmin/"
    );

    // Description shown in the VPN info endpoint
    private String tunnelDescription =
            "Simulated VPN tunnel — admin endpoints restricted to whitelisted IPs. " +
                    "Production: enforced via WireGuard/OpenVPN gateway.";

    // ── Getters and setters ───────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<String> getAllowedIps() { return allowedIps; }
    public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }

    public List<String> getProtectedPaths() { return protectedPaths; }
    public void setProtectedPaths(List<String> protectedPaths) { this.protectedPaths = protectedPaths; }

    public String getTunnelDescription() { return tunnelDescription; }
    public void setTunnelDescription(String tunnelDescription) { this.tunnelDescription = tunnelDescription; }
}