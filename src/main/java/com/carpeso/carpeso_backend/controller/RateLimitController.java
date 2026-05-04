package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Rate Limit Info Controller
 * Exposes a public endpoint so the professor can see the rate limit
 * configuration during demonstration.
 *
 * GET /api/public/security/rate-limits → returns the current limits
 */
@RestController
@RequestMapping("/api/public/security")
@CrossOrigin(origins = "*")
public class RateLimitController {

    @GetMapping("/rate-limits")
    public ResponseEntity<?> getRateLimits(HttpServletRequest request) {
        Map<String, Object> limits = Map.of(
                "description", "Carpeso rate limiting — sliding window per IP",
                "windowDuration", "60 seconds",
                "limits", Map.of(
                        "POST /api/auth/login",           "5 requests / 60s per IP",
                        "POST /api/auth/register",        "3 requests / 60s per IP",
                        "POST /api/auth/forgot-password", "3 requests / 60s per IP",
                        "POST /api/auth/verify-otp",      "5 requests / 60s per IP",
                        "All other endpoints",            "100 requests / 60s per IP"
                ),
                "onExceeded", Map.of(
                        "httpStatus",  429,
                        "response",    "429 Too Many Requests",
                        "headers",     "Retry-After, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset",
                        "behavior",    "Request blocked until window resets"
                ),
                "implementation", Map.of(
                        "strategy",   "Sliding window counter per IP address",
                        "storage",    "In-memory ConcurrentHashMap (no external dependency)",
                        "cleanup",    "Expired entries removed every 5 minutes",
                        "proxyAware", "Reads X-Forwarded-For and X-Real-IP headers"
                ),
                "yourIp", getClientIp(request)
        );
        return ResponseEntity.ok(ApiResponse.success("Rate limit configuration", limits));
    }

    private String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
            return forwarded.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}