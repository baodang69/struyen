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
public class RecordHistoryRequest {

    @NotBlank(message = "storyId khong duoc de trong")
    private String storyId;

    @NotBlank(message = "chapterId khong duoc de trong")
    private String chapterId;
}
