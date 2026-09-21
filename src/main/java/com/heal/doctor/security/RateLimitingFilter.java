package com.heal.doctor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory rate limiting filter.
 * Uses a sliding-window approach per IP address to protect sensitive public endpoints
 * from brute-force attacks, credential stuffing, and appointment spam.
 *
 * Rules enforced:
 *  - /api/v1/public/login            → 5 attempts per minute per IP
 *  - /api/v1/public/send-otp         → 3 attempts per minute per IP
 *  - /api/v1/public/forgot-password  → 3 attempts per minute per IP
 *  - /api/v1/public/appointments/book → 10 attempts per minute per IP
 *  - /api/v1/public/rogers/register  → 5 attempts per minute per IP
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final long WINDOW_MS = 60_000L; // 1 minute window

    // Limits: path prefix → max requests per minute per IP
    private static final Map<String, Integer> PATH_LIMITS = Map.of(
            "/api/v1/public/login",                5,
            "/api/v1/public/send-otp",             3,
            "/api/v1/public/forgot-password",      3,
            "/api/v1/public/appointments/book",    10,
            "/api/v1/public/rogers/register",      5,
            "/api/v1/public/signup",               5
    );

    // key = "IP::path", value = window data
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        Integer limit = getLimit(path);

        if (limit != null) {
            String clientIp = getClientIp(request);
            String key = clientIp + "::" + path;

            WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter());

            if (!counter.tryIncrement(limit, WINDOW_MS)) {
                logger.warn("Rate limit exceeded: ip={}, path={}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Too many requests. Please wait a moment and try again.\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Integer getLimit(String path) {
        for (Map.Entry<String, Integer> entry : PATH_LIMITS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take only the first IP (leftmost is the real client IP)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Simple sliding-window counter backed by atomics.
     * Thread-safe for concurrent requests.
     */
    private static class WindowCounter {
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger count = new AtomicInteger(0);

        boolean tryIncrement(int limit, long windowMs) {
            long now = System.currentTimeMillis();
            long start = windowStart.get();

            if (now - start > windowMs) {
                // New window — reset
                if (windowStart.compareAndSet(start, now)) {
                    count.set(1);
                    return true;
                }
            }

            int current = count.incrementAndGet();
            return current <= limit;
        }
    }
}
