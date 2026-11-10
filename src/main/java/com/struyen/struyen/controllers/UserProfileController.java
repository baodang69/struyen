package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.ChangePasswordRequest;
import com.struyen.struyen.dtos.UpdateProfileRequest;
import com.struyen.struyen.dtos.UserDto;
import com.struyen.struyen.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final AuthService authService;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Doi mat khau thanh cong."));
    }
}
