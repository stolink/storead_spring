package com.stolink.backend.domain.community.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CommunityPublishResponse {

    private UUID workId;
    private UUID chapterId;
    private boolean workCreated;  // 새 작품 생성 여부
}
