package com.stolink.backend.domain.like.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeResponse {

    private boolean liked;
    private long likeCount;

    public static LikeResponse of(boolean liked, long likeCount) {
        return LikeResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }
}
