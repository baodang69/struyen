package com.struyen.struyen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Bo loc gioi han tan suat goi API (Rate Limiting) nham chong tan cong vet can CPU (BCrypt DoS),
 * ngan spam dang ky tai khoan tu dong va flood binh luan.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_IPS = 10000;

    // Cau hinh gioi han: Max Requests trong khoang thoi gian Window (giay)
    private static final int LOGIN_MAX_REQUESTS = 10;
    private static final long LOGIN_WINDOW_MILLIS = 60 * 1000L; // 1 phut

    private static final int REGISTER_MAX_REQUESTS = 5;
    private static final long REGISTER_WINDOW_MILLIS = 300 * 1000L; // 5 phut

    private static final int COMMENT_MAX_REQUESTS = 10;
    private static final long COMMENT_WINDOW_MILLIS = 60 * 1000L; // 1 phut

    private final Map<String, ConcurrentLinkedQueue<Long>> requestLogs = new ConcurrentHashMap<>();
    private long lastCleanupTimestamp = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            String clientIp = getClientIp(request);

            if (path.equals("/api/v1/auth/login")) {
                if (!isAllowed(clientIp + ":login", LOGIN_MAX_REQUESTS, LOGIN_WINDOW_MILLIS)) {
                    sendRateLimitError(response, "Qua nhieu yeu cau dang nhap tu IP nay. Vui long thu lai sau 1 phut.");
                    return;
                }
            } else if (path.equals("/api/v1/auth/register")) {
                if (!isAllowed(clientIp + ":register", REGISTER_MAX_REQUESTS, REGISTER_WINDOW_MILLIS)) {
                    sendRateLimitError(response, "Qua nhieu yeu cau dang ky tu IP nay. Vui long thu lai sau vai phut.");
                    return;
                }
            } else if (path.equals("/api/v1/me/comments")) {
                if (!isAllowed(clientIp + ":comment", COMMENT_MAX_REQUESTS, COMMENT_WINDOW_MILLIS)) {
                    sendRateLimitError(response, "Ban dang gui binh luan qua nhanh. Vui long cho 1 phut roi thu lai.");
                    return;
                }
            }
        }

        periodicCleanup();
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedQueue<Long> timestamps = requestLogs.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // Loai bo cac timestamp da cu hon window
        while (!timestamps.isEmpty() && now - timestamps.peek() > windowMillis) {
            timestamps.poll();
        }

        if (timestamps.size() >= maxRequests) {
            log.warn("[Rate Limit Exceeded] Key: {}, Current: {}, Max: {}", key, timestamps.size(), maxRequests);
            return false;
        }

        timestamps.add(now);
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"" + message + "\"}");
    }

    private void periodicCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTimestamp > 300000L || requestLogs.size() > MAX_TRACKED_IPS) {
            lastCleanupTimestamp = now;
            requestLogs.entrySet().removeIf(entry -> {
                ConcurrentLinkedQueue<Long> queue = entry.getValue();
                while (!queue.isEmpty() && now - queue.peek() > 300000L) {
                    queue.poll();
                }
                return queue.isEmpty();
            });
        }
    }
}

