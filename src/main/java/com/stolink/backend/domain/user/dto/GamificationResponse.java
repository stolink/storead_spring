package com.stolink.backend.domain.user.dto;

import com.stolink.backend.domain.user.entity.UserGamification;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GamificationResponse {
    private int level;
    private int exp;
    private int maxExp;
    private String title;
    private int attendanceStreak;
    private boolean isTodayChecked;

    public static GamificationResponse from(UserGamification entity) {
        String title = "독자".equals(entity.getTitle()) ? entity.getUser().getNickname() : entity.getTitle();

        return GamificationResponse.builder()
                .level(entity.getLevel())
                .exp(entity.getExp())
                .maxExp(entity.getMaxExp())
                .title(title)
                .attendanceStreak(entity.getAttendanceStreak())
                .isTodayChecked(entity.isCheckedToday())
                .build();
    }
}
