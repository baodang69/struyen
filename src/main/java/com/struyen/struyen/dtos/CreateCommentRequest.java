package com.struyen.struyen.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "storyId khong duoc de trong")
    private String storyId;

    private String chapterId;

    private Long parentId;

    @NotBlank(message = "Noi dung binh luan khong duoc de trong")
    @Size(min = 1, max = 2000, message = "Noi dung binh luan tu 1 den 2000 ky tu")
    private String content;
}
