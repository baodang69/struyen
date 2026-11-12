package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.AuthResponse;
import com.struyen.struyen.dtos.LoginRequest;
import com.struyen.struyen.dtos.RefreshTokenRequest;
import com.struyen.struyen.dtos.RegisterRequest;
import com.struyen.struyen.dtos.UserDto;
import com.struyen.struyen.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long refreshExpirationMs;

    public AuthController(
            AuthService authService,
            @Value("${jwt.cookie.name:refreshToken}") String cookieName,
            @Value("${jwt.cookie.secure:false}") boolean cookieSecure,
            @Value("${jwt.cookie.same-site:Lax}") String cookieSameSite,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        ResponseCookie cookie = createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        ResponseCookie cookie = createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "${jwt.cookie.name:refreshToken}", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody) {

        String tokenToUse = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (requestBody != null ? requestBody.getRefreshToken() : null);

        if (tokenToUse == null || tokenToUse.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token khong duoc de trong.");
        }

        AuthResponse response = authService.refresh(tokenToUse);
        ResponseCookie cookie = createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${jwt.cookie.name:refreshToken}", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody) {

        String tokenToRevoke = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (requestBody != null ? requestBody.getRefreshToken() : null);

        if (tokenToRevoke != null && !tokenToRevoke.isBlank()) {
            authService.logout(tokenToRevoke);
        }

        ResponseCookie deleteCookie = createDeleteRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua xac thuc token.");
        }
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        if (refreshToken == null) {
            return createDeleteRefreshTokenCookie();
        }
        return ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(refreshExpirationMs / 1000)
                .sameSite(cookieSameSite)
                .build();
    }

    private ResponseCookie createDeleteRefreshTokenCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
    }
}
