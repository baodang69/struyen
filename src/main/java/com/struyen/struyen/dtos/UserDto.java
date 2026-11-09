package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Role;
import com.struyen.struyen.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String userStatus;
    private List<String> roles;
    private LocalDateTime createdAt;

    public static UserDto fromEntity(User user) {
        if (user == null) return null;

        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).toList()
                : Collections.emptyList();

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .userStatus(user.getUserStatus())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

