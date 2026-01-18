package com.stolink.backend.domain.user.service;

import com.stolink.backend.domain.user.dto.AttendanceResponse;
import com.stolink.backend.domain.user.dto.GamificationResponse;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.entity.UserGamification;
import com.stolink.backend.domain.user.repository.UserGamificationRepository;
import com.stolink.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamificationService {

    private final UserGamificationRepository gamificationRepository;
    private final UserRepository userRepository;

    public GamificationResponse getMyGamification(UUID userId) {
        UserGamification gamification = getOrCreateGamification(userId);
        return GamificationResponse.from(gamification);
    }

    @Transactional
    public AttendanceResponse checkAttendance(UUID userId) {
        UserGamification gamification = getOrCreateGamification(userId);
        
        int expGained = 50; 
        
        if (gamification.isCheckedToday()) {
             return AttendanceResponse.builder()
                .attendanceStreak(gamification.getAttendanceStreak())
                .expGained(0)
                .build();
        }

        gamification.markAttendance(LocalDate.now(), expGained);
        
        return AttendanceResponse.builder()
                .attendanceStreak(gamification.getAttendanceStreak())
                .expGained(expGained)
                .build();
    }

    private UserGamification getOrCreateGamification(UUID userId) {
        return gamificationRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));
                    
                    return gamificationRepository.save(UserGamification.builder()
                            .user(user)
                            .build());
                });
    }
}
