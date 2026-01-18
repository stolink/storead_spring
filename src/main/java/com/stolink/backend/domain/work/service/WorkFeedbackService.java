package com.stolink.backend.domain.work.service;

import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.dto.CreateFeedbackRequest;
import com.stolink.backend.domain.work.dto.WorkFeedbackResponse;
import com.stolink.backend.domain.work.entity.FeedbackType;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.entity.WorkFeedback;
import com.stolink.backend.domain.work.repository.WorkFeedbackRepository;
import com.stolink.backend.domain.work.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkFeedbackService {

    private final WorkFeedbackRepository feedbackRepository;
    private final WorkRepository workRepository;
    private final UserRepository userRepository;

    public WorkFeedbackResponse getFeedbackCounts(UUID workId, UUID userId) {
        if (!workRepository.existsById(workId)) {
            throw new IllegalArgumentException("Work not found");
        }

        List<Object[]> counts = feedbackRepository.countByWorkIdGroupByType(workId);
        
        Map<FeedbackType, Long> countMap = new HashMap<>();
        for (Object[] row : counts) {
            countMap.put((FeedbackType) row[0], (Long) row[1]);
        }

        List<String> activeActions = new ArrayList<>();
        if (userId != null) {
            List<WorkFeedback> myFeedbacks = feedbackRepository.findByWorkIdAndUserId(workId, userId);
            activeActions = myFeedbacks.stream()
                    .map(f -> f.getType().name().toLowerCase())
                    .collect(Collectors.toList());
        }

        return WorkFeedbackResponse.builder()
                .like(countMap.getOrDefault(FeedbackType.LIKE, 0L))
                .heart(countMap.getOrDefault(FeedbackType.HEART, 0L))
                .coffee(countMap.getOrDefault(FeedbackType.COFFEE, 0L))
                .next(countMap.getOrDefault(FeedbackType.NEXT, 0L))
                .activeActions(activeActions)
                .build();
    }

    @Transactional
    public void toggleFeedback(UUID workId, UUID userId, CreateFeedbackRequest request) {
        FeedbackType type;
        try {
            type = FeedbackType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid feedback type");
        }
        
        Optional<WorkFeedback> existing = feedbackRepository.findByWorkIdAndUserId(workId, userId)
                .stream().filter(f -> f.getType() == type).findFirst();
                
        if (existing.isPresent()) {
            feedbackRepository.delete(existing.get());
        } else {
             Work work = workRepository.findById(workId)
                .orElseThrow(() -> new IllegalArgumentException("Work not found"));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            feedbackRepository.save(WorkFeedback.builder()
                    .work(work)
                    .user(user)
                    .type(type)
                    .build());
        }
    }
}
