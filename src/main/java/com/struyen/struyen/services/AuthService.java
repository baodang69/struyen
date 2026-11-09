package com.struyen.struyen.services;

import com.struyen.struyen.dtos.AuthResponse;
import com.struyen.struyen.dtos.LoginRequest;
import com.struyen.struyen.dtos.RegisterRequest;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

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

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
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

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(UserDto.fromEntity(user))
                .build();
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
}
