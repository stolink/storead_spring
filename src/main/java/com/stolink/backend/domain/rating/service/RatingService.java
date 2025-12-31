package com.stolink.backend.domain.rating.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.rating.dto.RatingRequest;
import com.stolink.backend.domain.rating.dto.RatingResponse;
import com.stolink.backend.domain.rating.entity.ChapterRating;
import com.stolink.backend.domain.rating.repository.ChapterRatingRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService {

    private final ChapterRatingRepository chapterRatingRepository;
    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;
    private final UserRepository userRepository;

    @Transactional
    public RatingResponse rateChapter(UUID userId, UUID chapterId, RatingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        Work work = chapter.getWork();

        Optional<ChapterRating> existingRating = chapterRatingRepository.findByChapterIdAndUserId(chapterId, userId);

        if (existingRating.isPresent()) {
            ChapterRating rating = existingRating.get();
            int oldScore = rating.getScore();
            rating.updateScore(request.getScore());
            chapter.updateRating(oldScore, request.getScore());
            work.updateRating(oldScore, request.getScore());
        } else {
            ChapterRating rating = ChapterRating.builder()
                    .chapter(chapter)
                    .user(user)
                    .score(request.getScore())
                    .build();
            chapterRatingRepository.save(rating);
            chapter.addRating(request.getScore());
            work.addRating(request.getScore());
        }

        return RatingResponse.of(request.getScore(), chapter.getRatingSum(), chapter.getRatingCount());
    }

    public RatingResponse getChapterRating(UUID userId, UUID chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        Integer myScore = chapterRatingRepository.findByChapterIdAndUserId(chapterId, userId)
                .map(ChapterRating::getScore)
                .orElse(null);

        return RatingResponse.of(myScore, chapter.getRatingSum(), chapter.getRatingCount());
    }

    @Transactional
    public void deleteChapterRating(UUID userId, UUID chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        Work work = chapter.getWork();

        ChapterRating rating = chapterRatingRepository.findByChapterIdAndUserId(chapterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("별점을 찾을 수 없습니다"));

        chapter.removeRating(rating.getScore());
        work.removeRating(rating.getScore());
        chapterRatingRepository.delete(rating);
    }

    public RatingResponse getWorkRating(UUID workId) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        return RatingResponse.of(null, work.getRatingSum(), work.getRatingCount());
    }
}
