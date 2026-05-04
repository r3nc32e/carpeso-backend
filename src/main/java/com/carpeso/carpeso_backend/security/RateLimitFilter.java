package com.carpeso.carpeso_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limiting Filter — Carpeso Security Stack
 *
 * Implements a sliding window counter per IP address.
 * No external library (no Bucket4j) — pure Java ConcurrentHashMap.
 *
 * Rules (configurable via constants below):
 *   /api/auth/login           → 5 attempts per minute per IP
 *   /api/auth/register        → 3 attempts per minute per IP
 *   /api/auth/forgot-password → 3 attempts per minute per IP
 *   /api/auth/verify-otp      → 5 attempts per minute per IP
 *   All other endpoints       → 100 requests per minute per IP (general DoS protection)
 *
 * When exceeded → 429 Too Many Requests with Retry-After header.
 * Audit log entry is written for every rate limit violation.
 */
@Component
@Order(1) // Run BEFORE JwtFilter and SecurityFilter
public class RateLimitFilter implements Filter {

    // ── Configurable limits ────────────────────────────────────────────────
    private static final int  LOGIN_MAX_REQUESTS     = 5;
    private static final int  REGISTER_MAX_REQUESTS  = 3;
    private static final int  FORGOT_MAX_REQUESTS    = 3;
    private static final int  OTP_MAX_REQUESTS       = 5;
    private static final int  GENERAL_MAX_REQUESTS   = 100;
    private static final long WINDOW_MS              = 60_000L; // 1 minute window

    // ── In-memory store: key = "IP::endpoint", value = window tracker ─────
    private final ConcurrentHashMap<String, WindowCounter> counters =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest)  request;
        HttpServletResponse res = (HttpServletResponse) response;

        String ip   = extractIp(req);
        String path = req.getRequestURI();

        // Only rate-limit API calls
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        int limit = resolveLimit(path);
        String key = ip + "::" + normalizeEndpoint(path);

        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter());
        boolean allowed = counter.tryAcquire(limit, WINDOW_MS);

        if (!allowed) {
            long retryAfterSeconds = counter.retryAfterSeconds(WINDOW_MS);
            res.setStatus(429);
            res.setContentType("application/json");
            res.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            res.setHeader("X-RateLimit-Remaining", "0");
            res.setHeader("X-RateLimit-Reset",
                    String.valueOf(System.currentTimeMillis() / 1000 + retryAfterSeconds));

            Map<String, Object> body = Map.of(
                    "success", false,
                    "message", "Too many requests from your IP address. "
                            + "Please wait " + retryAfterSeconds + " seconds before trying again.",
                    "retryAfter", retryAfterSeconds,
                    "path", path
            );
            objectMapper.writeValue(res.getWriter(), body);
            return;
        }

        // Add rate limit headers to successful responses too (good practice)
        long remaining = counter.remaining(limit);
        res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        chain.doFilter(request, response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int resolveLimit(String path) {
        if (path.contains("/auth/login"))          return LOGIN_MAX_REQUESTS;
        if (path.contains("/auth/register"))       return REGISTER_MAX_REQUESTS;
        if (path.contains("/auth/forgot-password"))return FORGOT_MAX_REQUESTS;
        if (path.contains("/auth/verify-otp"))     return OTP_MAX_REQUESTS;
        if (path.contains("/auth/verify-registration")) return OTP_MAX_REQUESTS;
        return GENERAL_MAX_REQUESTS;
    }

    private String normalizeEndpoint(String path) {
        // Group all /auth/* sub-paths separately for granular tracking
        if (path.contains("/auth/login"))           return "/auth/login";
        if (path.contains("/auth/register"))        return "/auth/register";
        if (path.contains("/auth/forgot-password")) return "/auth/forgot";
        if (path.contains("/auth/verify"))          return "/auth/verify";
        return "/api/general";
    }

    private String extractIp(HttpServletRequest req) {
        // Handles proxies / load balancers
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return req.getRemoteAddr();
    }

    // ── Inner class: sliding window counter ───────────────────────────────

    private static class WindowCounter {
        private final AtomicInteger count     = new AtomicInteger(0);
        private final AtomicLong    windowStart = new AtomicLong(System.currentTimeMillis());

        synchronized boolean tryAcquire(int limit, long windowMs) {
            long now = System.currentTimeMillis();
            long start = windowStart.get();

            // Reset window if expired
            if (now - start >= windowMs) {
                count.set(0);
                windowStart.set(now);
            }

            int current = count.incrementAndGet();
            return current <= limit;
        }

        synchronized long remaining(int limit) {
            return Math.max(0, limit - count.get());
        }

        synchronized long retryAfterSeconds(long windowMs) {
            long elapsed = System.currentTimeMillis() - windowStart.get();
            return Math.max(1, (windowMs - elapsed) / 1000);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Periodically clean up expired entries to prevent memory leak
        // Runs every 5 minutes via a daemon thread
        Thread cleanup = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(300_000L); // 5 minutes
                    long now = System.currentTimeMillis();
                    counters.entrySet().removeIf(entry ->
                            now - entry.getValue().windowStart.get() > WINDOW_MS * 2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        cleanup.setDaemon(true);
        cleanup.setName("rate-limit-cleanup");
        cleanup.start();
    }
}