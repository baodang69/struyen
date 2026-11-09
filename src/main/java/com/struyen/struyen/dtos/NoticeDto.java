package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Notice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeDto {
    private Long id;
    private String title;
    private String content;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static NoticeDto fromEntity(Notice notice) {
        if (notice == null) return null;
        return NoticeDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .isActive(notice.getIsActive())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
