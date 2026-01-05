package com.stolink.backend.domain.like.runner;

import com.stolink.backend.domain.like.repository.WorkLikeRepository;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncLikeCountRunner implements CommandLineRunner {

    private final WorkRepository workRepository;
    private final WorkLikeRepository workLikeRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("[SyncLikeCountRunner] Starting like count synchronization...");

        List<Work> works = workRepository.findAll();
        int updatedCount = 0;

        for (Work work : works) {
            // 실제 좋아요 수 집계
            long actualLikeCount = workLikeRepository.countByWorkId(work.getId());

            // 캐시된 값과 다르면 업데이트
            if (work.getLikeCount() != actualLikeCount) {
                work.syncLikeCount(actualLikeCount);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.info("[SyncLikeCountRunner] Synced like counts for {} works.", updatedCount);
        } else {
            log.info("[SyncLikeCountRunner] All like counts are already in sync.");
        }
        log.info("[SyncLikeCountRunner] Like count synchronization completed.");
    }
}
