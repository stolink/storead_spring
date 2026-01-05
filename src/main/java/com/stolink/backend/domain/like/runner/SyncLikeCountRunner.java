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
     * 좋아요 수 동기화 실행 여부 (기본값: false)
     * - 주의: true로 설정 시 애플리케이션 시작 시 전체 Works 테이블에 락(Table Lock)을 유발할 수 있음.
     * - 데이터가 많은 운영 환경에서는 false로 유지하고, 별도의 배치 작업이나 관리자 기능을 통해 실행 권장.
     */
    @Value("${app.sync.like-count.enabled:false}")
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
