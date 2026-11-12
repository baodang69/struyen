package com.struyen.struyen.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RefreshTokenService refreshTokenService;

    private static final long REFRESH_EXPIRATION_MS = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(redisTemplate, REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("createRefreshToken should generate token and store it in Redis with TTL")
    void createRefreshToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        String token = refreshTokenService.createRefreshToken("testuser");

        assertThat(token).isNotBlank();
        verify(valueOperations).set(eq(RefreshTokenService.TOKEN_KEY_PREFIX + token), eq("testuser"), eq(REFRESH_EXPIRATION_MS), eq(TimeUnit.MILLISECONDS));
        verify(setOperations).add(eq(RefreshTokenService.USER_TOKENS_PREFIX + "testuser"), eq(token));
        verify(redisTemplate).expire(eq(RefreshTokenService.USER_TOKENS_PREFIX + "testuser"), eq(REFRESH_EXPIRATION_MS), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("validateAndGetUsername should return username when token exists in Redis")
    void validateAndGetUsername_ValidToken_ReturnsUsername() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RefreshTokenService.TOKEN_KEY_PREFIX + "valid-token")).thenReturn("testuser");

        String username = refreshTokenService.validateAndGetUsername("valid-token");

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("validateAndGetUsername should return null when token is blank or not found")
    void validateAndGetUsername_InvalidOrBlank_ReturnsNull() {
        assertThat(refreshTokenService.validateAndGetUsername(null)).isNull();
        assertThat(refreshTokenService.validateAndGetUsername("   ")).isNull();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RefreshTokenService.TOKEN_KEY_PREFIX + "not-found")).thenReturn(null);

        assertThat(refreshTokenService.validateAndGetUsername("not-found")).isNull();
    }

    @Test
    @DisplayName("rotateRefreshToken should revoke old token and return new token")
    void rotateRefreshToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(RefreshTokenService.TOKEN_KEY_PREFIX + "old-token")).thenReturn("testuser");

        RefreshTokenService.RotationResult result = refreshTokenService.rotateRefreshToken("old-token");

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.newToken()).isNotBlank().isNotEqualTo("old-token");

        // Verify old token is deleted
        verify(redisTemplate).delete(RefreshTokenService.TOKEN_KEY_PREFIX + "old-token");
        verify(setOperations).remove(RefreshTokenService.USER_TOKENS_PREFIX + "testuser", "old-token");

        // Verify new token is stored
        verify(valueOperations).set(eq(RefreshTokenService.TOKEN_KEY_PREFIX + result.newToken()), eq("testuser"), eq(REFRESH_EXPIRATION_MS), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("rotateRefreshToken should throw UNAUTHORIZED when old token is invalid or expired")
    void rotateRefreshToken_InvalidToken_ThrowsUnauthorized() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RefreshTokenService.TOKEN_KEY_PREFIX + "expired-token")).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("expired-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("revokeAllUserTokens should delete all tokens associated with the user")
    void revokeAllUserTokens_Success() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(RefreshTokenService.USER_TOKENS_PREFIX + "testuser"))
                .thenReturn(Set.of("token1", "token2"));

        refreshTokenService.revokeAllUserTokens("testuser");

        verify(redisTemplate).delete(argThat((java.util.List<String> list) ->
                list.contains(RefreshTokenService.TOKEN_KEY_PREFIX + "token1") &&
                list.contains(RefreshTokenService.TOKEN_KEY_PREFIX + "token2")
        ));
        verify(redisTemplate).delete(RefreshTokenService.USER_TOKENS_PREFIX + "testuser");
    }

    @Test
    @DisplayName("revokeAllUserTokens should do nothing gracefully when user has no tokens")
    void revokeAllUserTokens_NoTokens_Graceful() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(RefreshTokenService.USER_TOKENS_PREFIX + "emptyuser"))
                .thenReturn(Collections.emptySet());

        refreshTokenService.revokeAllUserTokens("emptyuser");

        verify(redisTemplate).delete(RefreshTokenService.USER_TOKENS_PREFIX + "emptyuser");
    }
}

