package com.stolink.backend.domain.rating.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RatingResponse {

    private Integer myScore;
    private Double averageScore;
    private Long ratingCount;

    public static RatingResponse of(Integer myScore, Long ratingSum, Long ratingCount) {
        Double average = ratingCount > 0 ? (double) ratingSum / ratingCount : 0.0;
        return RatingResponse.builder()
                .myScore(myScore)
                .averageScore(Math.round(average * 10) / 10.0)
                .ratingCount(ratingCount)
                .build();
    }
}
