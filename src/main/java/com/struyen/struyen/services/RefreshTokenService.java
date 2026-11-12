package com.struyen.struyen.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service quan ly vong doi Refresh Token tren Redis:
 * - Cap phat Refresh Token ngau nhien co thoi gian song (TTL)
 * - Xoay vong token (Refresh Token Rotation) nham phong chong Replay Attack
 * - Thu hoi token don le (Logout) hoac toan bo token cua User (Doi mat khau)
 */
@Service
@Slf4j
public class RefreshTokenService {

    public static final String TOKEN_KEY_PREFIX = "refresh_token:";
    public static final String USER_TOKENS_PREFIX = "user_refresh_tokens:";

    private final StringRedisTemplate redisTemplate;
    private final long refreshExpirationMs;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.redisTemplate = redisTemplate;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Tao moi mot Refresh Token va luu vao Redis voi thoi gian song (TTL)
     */
    public String createRefreshToken(String username) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        try {
            String tokenKey = TOKEN_KEY_PREFIX + token;
            String userKey = USER_TOKENS_PREFIX + username;

            redisTemplate.opsForValue().set(tokenKey, username, refreshExpirationMs, TimeUnit.MILLISECONDS);
            redisTemplate.opsForSet().add(userKey, token);
            redisTemplate.expire(userKey, refreshExpirationMs, TimeUnit.MILLISECONDS);

            log.info("Da tao Refresh Token moi cho user: {}", username);
            return token;
        } catch (Exception e) {
            log.error("Loi khi luu Refresh Token vao Redis cho user {}: {}", username, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "He thong xac thuc Redis tam thoi khong kha dung.");
        }
    }

    /**
     * Kiem tra tinh hop le va lay username tu Refresh Token
     */
    public String validateAndGetUsername(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        } catch (Exception e) {
            log.error("Loi khi truy van Refresh Token tu Redis: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Xoay vong Refresh Token (Refresh Token Rotation):
     * Xoa token cu, tao va tra ve token moi
     */
    public RotationResult rotateRefreshToken(String oldToken) {
        String username = validateAndGetUsername(oldToken);
        if (username == null) {
            log.warn("Yeu cau refresh bi tu choi do token khong ton tai hoac da het han");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn hoặc không hợp lệ.");
        }

        revokeRefreshToken(oldToken, username);
        String newToken = createRefreshToken(username);

        return new RotationResult(username, newToken);
    }

    /**
     * Thu hoi mot Refresh Token cu the
     */
    public void revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String username = validateAndGetUsername(token);
        if (username != null) {
            revokeRefreshToken(token, username);
        } else {
            try {
                redisTemplate.delete(TOKEN_KEY_PREFIX + token);
            } catch (Exception e) {
                log.error("Loi khi xoa token don le tren Redis: {}", e.getMessage());
            }
        }
    }

    /**
     * Thu hoi Refresh Token kem cap nhat Set cua user
     */
    public void revokeRefreshToken(String token, String username) {
        try {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
            if (username != null && !username.isBlank()) {
                redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + username, token);
            }
            log.info("Da thu hoi Refresh Token cua user: {}", username);
        } catch (Exception e) {
            log.error("Loi khi thu hoi Refresh Token tren Redis: {}", e.getMessage());
        }
    }

    /**
     * Thu hoi toan bo Refresh Token cua mot nguoi dung (dung khi doi mat khau)
     */
    public void revokeAllUserTokens(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            String userKey = USER_TOKENS_PREFIX + username;
            Set<String> tokens = redisTemplate.opsForSet().members(userKey);
            if (tokens != null && !tokens.isEmpty()) {
                List<String> tokenKeys = tokens.stream()
                        .map(t -> TOKEN_KEY_PREFIX + t)
                        .toList();
                redisTemplate.delete(tokenKeys);
            }
            redisTemplate.delete(userKey);
            log.info("Da thu hoi toan bo Refresh Token cua user: {}", username);
        } catch (Exception e) {
            log.error("Loi khi thu hoi toan bo Refresh Token tren Redis cua user {}: {}", username, e.getMessage());
        }
    }

    /**
     * Record ket qua xoay vong token
     */
    public record RotationResult(String username, String newToken) {
    }
}

