package com.stolink.backend.domain.like.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeResponse {

    @JsonProperty("isLiked")
    private boolean isLiked;
    private long likeCount;

    public static LikeResponse of(boolean isLiked, long likeCount) {
        return LikeResponse.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }
}
