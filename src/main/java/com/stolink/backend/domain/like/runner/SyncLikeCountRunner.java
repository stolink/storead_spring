package com.stolink.backend.domain.like.runner;

import com.stolink.backend.domain.work.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 모든 작품의 좋아요 수를 실제 데이터와 동기화
 * - N+1 문제 해결: 단일 벌크 업데이트 쿼리로 처리
 * - OOM 방지: 전체 작품을 메모리에 로드하지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncLikeCountRunner implements CommandLineRunner {

    private final WorkRepository workRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("[SyncLikeCountRunner] Starting like count synchronization...");

        // 단일 벌크 업데이트 쿼리로 모든 작품의 좋아요 수 동기화
        // N+1 문제 해결 및 OOM 방지
        workRepository.syncAllLikeCounts();

        log.info("[SyncLikeCountRunner] Like count synchronization completed.");
    }
}
