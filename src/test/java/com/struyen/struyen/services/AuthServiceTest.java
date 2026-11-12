package com.struyen.struyen.services;

import com.struyen.struyen.dtos.AuthResponse;
import com.struyen.struyen.dtos.ChangePasswordRequest;
import com.struyen.struyen.dtos.LoginRequest;
import com.struyen.struyen.dtos.RegisterRequest;
import com.struyen.struyen.entities.Role;
import com.struyen.struyen.entities.User;
import com.struyen.struyen.repositories.RoleRepository;
import com.struyen.struyen.repositories.UserRepository;
import com.struyen.struyen.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Role memberRole;

    @BeforeEach
    void setUp() {
        memberRole = Role.builder().id(1).name("ROLE_MEMBER").description("Member").build();
        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .userStatus("ACTIVE")
                .roles(new HashSet<>(Set.of(memberRole)))
                .build();
    }

    @Test
    @DisplayName("register should create user, return access token and refresh token")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateTokenFromUsername(eq("testuser"), anyList())).thenReturn("mock-access-token");
        when(refreshTokenService.createRefreshToken("testuser")).thenReturn("mock-refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("login should authenticate and return both access token and refresh token")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123")
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("mock-access-token");
        when(userRepository.findWithRolesByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(refreshTokenService.createRefreshToken("testuser")).thenReturn("mock-refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        verify(refreshTokenService).createRefreshToken("testuser");
    }

    @Test
    @DisplayName("refresh should rotate token, generate new access token and return response")
    void refresh_Success() {
        when(refreshTokenService.rotateRefreshToken("old-refresh-token"))
                .thenReturn(new RefreshTokenService.RotationResult("testuser", "new-refresh-token"));
        when(userRepository.findWithRolesByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateTokenFromUsername(eq("testuser"), anyList())).thenReturn("new-access-token");

        AuthResponse response = authService.refresh("old-refresh-token");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).rotateRefreshToken("old-refresh-token");
    }

    @Test
    @DisplayName("refresh with blank token should throw BAD_REQUEST")
    void refresh_BlankToken_ThrowsBadRequest() {
        assertThatThrownBy(() -> authService.refresh(""))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("refresh for banned user should revoke token and throw FORBIDDEN")
    void refresh_BannedUser_ThrowsForbidden() {
        sampleUser.setUserStatus("BANNED");
        when(refreshTokenService.rotateRefreshToken("some-token"))
                .thenReturn(new RefreshTokenService.RotationResult("testuser", "fresh-token"));
        when(userRepository.findWithRolesByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.refresh("some-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(refreshTokenService).revokeRefreshToken("fresh-token", "testuser");
    }

    @Test
    @DisplayName("logout should call revokeRefreshToken on RefreshTokenService")
    void logout_Success() {
        authService.logout("valid-refresh-token");

        verify(refreshTokenService).revokeRefreshToken("valid-refresh-token");
    }

    @Test
    @DisplayName("changePassword should update password and revoke all user tokens")
    void changePassword_RevokesAllTokens() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPass")
                .newPassword("newPass123")
                .build();

        when(userRepository.findWithRolesByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPass", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newHashedPassword");

        authService.changePassword("testuser", request);

        verify(userRepository).save(sampleUser);
        verify(refreshTokenService).revokeAllUserTokens("testuser");
        assertThat(sampleUser.getPasswordHash()).isEqualTo("newHashedPassword");
    }
}

