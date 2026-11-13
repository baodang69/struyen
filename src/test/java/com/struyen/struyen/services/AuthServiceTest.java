package com.struyen.struyen.services;

import com.struyen.struyen.dtos.AuthResponse;
import com.struyen.struyen.dtos.ChangePasswordRequest;
import com.struyen.struyen.dtos.GoogleLoginRequest;
import com.struyen.struyen.dtos.GoogleTokenInfoDto;
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

    @Mock
    private GoogleAuthService googleAuthService;

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

    @Test
    @DisplayName("loginWithGoogle should create new user when not existing")
    void loginWithGoogle_NewUser_Success() {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .credential("valid-google-id-token")
                .build();

        GoogleTokenInfoDto tokenInfo = GoogleTokenInfoDto.builder()
                .email("newgoogle@example.com")
                .emailVerified("true")
                .name("Google Newbie")
                .sub("google-sub-12345")
                .picture("https://lh3.googleusercontent.com/avatar.png")
                .build();

        when(googleAuthService.verifyToken("valid-google-id-token")).thenReturn(tokenInfo);
        when(userRepository.findWithRolesByAuthProviderAndProviderId("GOOGLE", "google-sub-12345")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("newgoogle@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode(anyString())).thenReturn("randomHashedPassword");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        User savedUser = User.builder()
                .id(2L)
                .username("googlenewbie")
                .email("newgoogle@example.com")
                .passwordHash("randomHashedPassword")
                .authProvider("GOOGLE")
                .providerId("google-sub-12345")
                .avatarUrl("https://lh3.googleusercontent.com/avatar.png")
                .userStatus("ACTIVE")
                .roles(new HashSet<>(Set.of(memberRole)))
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateTokenFromUsername(eq("googlenewbie"), anyList())).thenReturn("jwt-google-access-token");
        when(refreshTokenService.createRefreshToken("googlenewbie")).thenReturn("jwt-google-refresh-token");

        AuthResponse response = authService.loginWithGoogle(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-google-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("jwt-google-refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("newgoogle@example.com");
        assertThat(response.getUser().getUsername()).isEqualTo("googlenewbie");
    }

    @Test
    @DisplayName("loginWithGoogle should throw CONFLICT when email was already registered as LOCAL account")
    void loginWithGoogle_ExistingLocalUser_ThrowsConflict() {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .credential("valid-google-id-token")
                .build();

        GoogleTokenInfoDto tokenInfo = GoogleTokenInfoDto.builder()
                .email("test@example.com")
                .emailVerified("true")
                .name("Test User")
                .sub("google-sub-67890")
                .picture("https://lh3.googleusercontent.com/avatar2.png")
                .build();

        when(googleAuthService.verifyToken("valid-google-id-token")).thenReturn(tokenInfo);
        when(userRepository.findWithRolesByAuthProviderAndProviderId("GOOGLE", "google-sub-67890")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.loginWithGoogle(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("loginWithGoogle should succeed for existing Google user")
    void loginWithGoogle_ExistingGoogleUser_Success() {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .credential("valid-google-id-token")
                .build();

        GoogleTokenInfoDto tokenInfo = GoogleTokenInfoDto.builder()
                .email("test@example.com")
                .emailVerified("true")
                .name("Test User")
                .sub("google-sub-67890")
                .picture("https://lh3.googleusercontent.com/avatar2.png")
                .build();

        User existingGoogleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .authProvider("GOOGLE")
                .providerId("google-sub-67890")
                .avatarUrl("https://lh3.googleusercontent.com/avatar2.png")
                .userStatus("ACTIVE")
                .roles(new HashSet<>(Set.of(memberRole)))
                .build();

        when(googleAuthService.verifyToken("valid-google-id-token")).thenReturn(tokenInfo);
        when(userRepository.findWithRolesByAuthProviderAndProviderId("GOOGLE", "google-sub-67890")).thenReturn(Optional.of(existingGoogleUser));
        when(jwtTokenProvider.generateTokenFromUsername(eq("testuser"), anyList())).thenReturn("jwt-google-token");
        when(refreshTokenService.createRefreshToken("testuser")).thenReturn("jwt-google-refresh-token");

        AuthResponse response = authService.loginWithGoogle(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-google-token");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("loginWithGoogle should throw FORBIDDEN if user is banned")
    void loginWithGoogle_BannedUser_ThrowsForbidden() {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .credential("valid-google-id-token")
                .build();

        GoogleTokenInfoDto tokenInfo = GoogleTokenInfoDto.builder()
                .email("banned@example.com")
                .emailVerified("true")
                .name("Banned User")
                .sub("google-sub-banned")
                .build();

        User bannedUser = User.builder()
                .id(3L)
                .username("banneduser")
                .email("banned@example.com")
                .authProvider("GOOGLE")
                .providerId("google-sub-banned")
                .userStatus("BANNED")
                .roles(new HashSet<>(Set.of(memberRole)))
                .build();

        when(googleAuthService.verifyToken("valid-google-id-token")).thenReturn(tokenInfo);
        when(userRepository.findWithRolesByAuthProviderAndProviderId("GOOGLE", "google-sub-banned")).thenReturn(Optional.of(bannedUser));
        when(roleRepository.findByName("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));

        assertThatThrownBy(() -> authService.loginWithGoogle(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }
}

