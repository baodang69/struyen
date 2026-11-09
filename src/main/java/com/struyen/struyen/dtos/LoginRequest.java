package com.struyen.struyen.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username hoac email khong duoc de trong")
    private String usernameOrEmail;

    @NotBlank(message = "Password khong duoc de trong")
    private String password;
}
