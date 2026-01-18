package com.stolink.backend.domain.user.entity;

import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_gamifications")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserGamification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer exp = 0;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String title = "독자";

    @Column(nullable = false)
    @Builder.Default
    private Integer attendanceStreak = 0;

    @Column
    private LocalDate lastAttendanceDate;

    public void markAttendance(LocalDate date, int expGained) {
        if (date.equals(lastAttendanceDate)) {
            return;
        }

        if (lastAttendanceDate != null && lastAttendanceDate.plusDays(1).equals(date)) {
            attendanceStreak++;
        } else {
            attendanceStreak = 1;
        }
        
        this.lastAttendanceDate = date;
        addExp(expGained);
    }

    public void addExp(int amount) {
        this.exp += amount;
        // Simple level up logic: Level * 100 exp needed for next level
        int requiredExp = this.level * 100; // Simplified maxExp logic
        while (this.exp >= requiredExp) {
            this.exp -= requiredExp;
            this.level++;
            requiredExp = this.level * 100;
        }
    }
    
    public int getMaxExp() {
        return this.level * 100;
    }
    
    public boolean isCheckedToday() {
        return LocalDate.now().equals(lastAttendanceDate);
    }
}
