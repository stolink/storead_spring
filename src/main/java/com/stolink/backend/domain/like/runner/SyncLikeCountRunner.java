package com.stolink.backend.domain.like.runner;

import com.stolink.backend.domain.work.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 모든 작품의 좋아요 수를 실제 데이터와 동기화
 * - N+1 문제 해결: 단일 벌크 업데이트 쿼리로 처리
 * - OOM 방지: 전체 작품을 메모리에 로드하지 않음
 * - 분산 환경 중복 실행 방지: 환경 변수로 조건부 실행 제어
 * (쿠버네티스 등에서는 하나의 Pod에서만 실행하도록 설정)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncLikeCountRunner implements CommandLineRunner {

    private final WorkRepository workRepository;

    /**
     * 좋아요 수 동기화 실행 여부 (기본값: true)
     * 분산 환경에서는 하나의 인스턴스에서만 true로 설정
     * 예: SYNC_LIKE_COUNT_ENABLED=false
     */
    @Value("${app.sync.like-count.enabled:true}")
    private boolean syncEnabled;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!syncEnabled) {
            log.info("[SyncLikeCountRunner] Like count synchronization is DISABLED on this instance.");
            return;
        }

        log.info("[SyncLikeCountRunner] Starting like count synchronization...");

        // 단일 벌크 업데이트 쿼리로 모든 작품의 좋아요 수 동기화
        // N+1 문제 해결 및 OOM 방지
        workRepository.syncAllLikeCounts();

        log.info("[SyncLikeCountRunner] Like count synchronization completed.");
    }
}
