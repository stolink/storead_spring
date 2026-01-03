package com.stolink.backend.domain.community.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CommunityPublishRequest {

    @NotNull(message = "draftId는 필수입니다")
    private UUID draftId;

    private Integer chapterNumber;  // null이면 자동 할당

    private String title;           // 선택: 챕터 제목 수동 설정용
}
