package com.stolink.backend.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceResponse {
    private int attendanceStreak;
    private int expGained;
}
