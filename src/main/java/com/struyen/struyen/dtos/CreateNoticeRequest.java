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
public class CreateNoticeRequest {

    @NotBlank(message = "Tieu de thong bao khong duoc de trong")
    private String title;

    @NotBlank(message = "Noi dung thong bao khong duoc de trong")
    private String content;

    @Builder.Default
    private Boolean isActive = true;
}
