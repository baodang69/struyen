package com.struyen.struyen.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username khong duoc de trong")
    @Size(min = 3, max = 50, message = "Username phai tu 3 den 50 ky tu")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username chi duoc chua chu cai, chu so va dau gach duoi")
    private String username;

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    @Size(max = 100, message = "Email toi da 100 ky tu")
    private String email;

    @NotBlank(message = "Password khong duoc de trong")
    @Size(min = 6, max = 100, message = "Password phai co it nhat 6 ky tu")
    private String password;
}

