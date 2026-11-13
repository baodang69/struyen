package com.struyen.struyen.services;

import com.struyen.struyen.dtos.AuthResponse;
import com.struyen.struyen.dtos.ChangePasswordRequest;
import com.struyen.struyen.dtos.GoogleLoginRequest;
import com.struyen.struyen.dtos.GoogleTokenInfoDto;
import com.struyen.struyen.dtos.LoginRequest;
import com.struyen.struyen.dtos.RegisterRequest;
import com.struyen.struyen.dtos.UpdateProfileRequest;
import com.struyen.struyen.dtos.UserDto;
import com.struyen.struyen.entities.Role;
import com.struyen.struyen.entities.User;
import com.struyen.struyen.repositories.RoleRepository;
import com.struyen.struyen.repositories.UserRepository;
import com.struyen.struyen.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final GoogleAuthService googleAuthService;

    public static final String DEFAULT_ROLE = "ROLE_MEMBER";
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    /**
     * Dang ky tai khoan moi
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username da ton tai trong he thong.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email da duoc su dung boi tai khoan khac.");
        }

        Role memberRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(DEFAULT_ROLE)
                        .description("Quyen thanh vien doc truyen tieu chuan")
                        .build()));

        User user = User.builder()
                .username(request.getUsername().trim().toLowerCase())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .userStatus("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(memberRole)))
                .build();

        user = userRepository.save(user);

        List<String> roleNames = List.of(memberRole.getName());
        String token = jwtTokenProvider.generateTokenFromUsername(user.getUsername(), roleNames);
        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .refreshToken(refreshToken)
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Dang nhap bang username hoac email
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsernameOrEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findWithRolesByUsername(identifier)
                .or(() -> userRepository.findWithRolesByEmail(identifier))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay thong tin nguoi dung."));

        if ("BANNED".equalsIgnoreCase(user.getUserStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan nay da bi khoa do vi pham chinh sach.");
        }

        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .refreshToken(refreshToken)
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Dang nhap hoac dang ky tu dong bang Google ID Token
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenInfoDto tokenInfo = googleAuthService.verifyToken(request.getCredential());

        String email = tokenInfo.getEmail().trim().toLowerCase();
        String googleSub = tokenInfo.getSub().trim();
        String name = tokenInfo.getName();
        String picture = tokenInfo.getPicture();

        // 1. Tim user da tung lien ket voi Google account nay
        User user = userRepository.findWithRolesByAuthProviderAndProviderId("GOOGLE", googleSub)
                .orElse(null);

        Role memberRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(DEFAULT_ROLE)
                        .description("Quyen thanh vien doc truyen tieu chuan")
                        .build()));

        if (user == null) {
            // 2. Kiem tra de phong Account Pre-Hijacking neu email da duoc dang ky truoc bang tai khoan thuong
            if (userRepository.existsByEmail(email)) {
                log.warn("Ngan chan Account Pre-Hijacking: Email {} da duoc dang ky truoc bang tai khoan thuong", email);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Email này đã được đăng ký bằng tài khoản thường (mật khẩu). Để bảo vệ tài khoản, vui lòng đăng nhập bằng mật khẩu.");
            }

            // 3. Email hoan toan moi: Khoi tao tai khoan Google moi
            String username = generateUniqueUsername(name, email);

            user = User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .authProvider("GOOGLE")
                    .providerId(googleSub)
                    .avatarUrl(picture)
                    .userStatus("ACTIVE")
                    .roles(new HashSet<>(Collections.singletonList(memberRole)))
                    .build();

            user = userRepository.save(user);
            log.info("Da tao tai khoan nguoi dung moi tu Google OAuth: {}", username);
        } else {
            // User da lien ket Google: cap nhat avatar neu co thay doi
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && picture != null) {
                user.setAvatarUrl(picture);
                User saved = userRepository.save(user);
                if (saved != null) {
                    user = saved;
                }
                log.info("Da cap nhat avatar Google OAuth cho user: {}", user.getUsername());
            }
        }

        if ("BANNED".equalsIgnoreCase(user.getUserStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan nay da bi khoa do vi pham chinh sach.");
        }

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        String accessToken = jwtTokenProvider.generateTokenFromUsername(user.getUsername(), roleNames);
        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .token(accessToken)
                .tokenType("Bearer")
                .refreshToken(refreshToken)
                .user(UserDto.fromEntity(user))
                .build();
    }

    private String generateUniqueUsername(String name, String email) {
        String baseName = null;
        if (name != null && !name.isBlank()) {
            baseName = Normalizer.normalize(name, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
        }
        if (baseName == null || baseName.isBlank()) {
            baseName = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        }
        if (baseName.isBlank()) {
            baseName = "user";
        }
        if (baseName.length() > 30) {
            baseName = baseName.substring(0, 30);
        }

        String candidate = baseName;
        int counter = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = baseName + (int) (Math.random() * 9000 + 1000);
            counter++;
            if (counter > 10) {
                candidate = "user_" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return candidate;
    }

    /**
     * Gia han Access Token bang Refresh Token (kem Refresh Token Rotation)
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token khong duoc de trong.");
        }

        RefreshTokenService.RotationResult result = refreshTokenService.rotateRefreshToken(refreshToken);

        User user = userRepository.findWithRolesByUsername(result.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Khong tim thay thong tin nguoi dung."));

        if ("BANNED".equalsIgnoreCase(user.getUserStatus())) {
            refreshTokenService.revokeRefreshToken(result.newToken(), user.getUsername());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan nay da bi khoa do vi pham chinh sach.");
        }

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        String newAccessToken = jwtTokenProvider.generateTokenFromUsername(user.getUsername(), roleNames);

        return AuthResponse.builder()
                .token(newAccessToken)
                .tokenType("Bearer")
                .refreshToken(result.newToken())
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Dang xuat: Thu hoi Refresh Token
     */
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
    }

    /**
     * Lay thong tin profile cua user dang dang nhap
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String username) {
        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay thong tin tai khoan."));

        return UserDto.fromEntity(user);
    }

    /**
     * Cap nhat thong tin ca nhan (avatar)
     */
    @Transactional
    public UserDto updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay thong tin tai khoan."));

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        user = userRepository.save(user);
        log.info("User {} da cap nhat ho so ca nhan", username);
        return UserDto.fromEntity(user);
    }

    /**
     * Doi mat khau nguoi dung
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay thong tin tai khoan."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mat khau hien tai khong dung.");
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mat khau moi khong duoc trung voi mat khau hien tai.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(username);
        log.info("User {} da doi mat khau thanh cong va thu hoi toan bo Refresh Token", username);
    }
}

