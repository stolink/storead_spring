package com.stolink.backend.domain.chapter.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 챕터 중복 체크 서비스
 * 
 * ChapterRepository의 네이티브 쿼리에서 List<String>를 PostgreSQL text[]로
 * 캐스팅할 수 없는 문제(ClassCastException)를 해결하기 위해
 * Java 코드로 중복 체크 로직을 구현합니다.
 * 
 * 참고: DocumentPublishService에서도 동일한 패턴으로 해결한 이력이 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterDuplicationChecker {

    private final ChapterRepository chapterRepository;

    /**
     * 주어진 documentIds 중 이미 게시된 것이 있는지 확인
     * 
     * N+1 방지를 위해 workId로 모든 챕터를 한 번에 조회 후 메모리에서 비교합니다.
     * 
     * @param workId 작품 ID
     * @param docIds 확인할 문서 ID 목록
     * @return 중복이 있으면 true
     */
    public boolean hasAnyDuplicates(UUID workId, List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return false;
        }

        // 1. 해당 Work의 모든 챕터 조회 (한 번의 쿼리)
        List<Chapter> chapters = chapterRepository.findByWorkIdOrderByChapterNumberAsc(workId);

        if (chapters.isEmpty()) {
            return false;
        }

        // 2. 입력 docIds를 Set으로 변환하여 O(1) 조회
        Set<String> docIdSet = new HashSet<>(docIds);

        // 3. 메모리에서 중복 체크
        for (Chapter chapter : chapters) {
            // 단일 documentId 체크
            if (chapter.getDocumentId() != null && docIdSet.contains(chapter.getDocumentId())) {
                log.debug("Duplicate found: documentId={} in chapterId={}",
                        chapter.getDocumentId(), chapter.getId());
                return true;
            }

            // 병합 배포된 documentIds 배열 체크
            if (chapter.getDocumentIds() != null) {
                for (String existingId : chapter.getDocumentIds()) {
                    if (docIdSet.contains(existingId)) {
                        log.debug("Duplicate found in merged chapter: documentId={} in chapterId={}",
                                existingId, chapter.getId());
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 중복된 documentId 목록 반환 (에러 메시지용)
     * 
     * @param workId 작품 ID
     * @param docIds 확인할 문서 ID 목록
     * @return 중복된 문서 ID 목록 (중복 제거됨)
     */
    public List<String> findDuplicates(UUID workId, List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return List.of();
        }

        List<Chapter> chapters = chapterRepository.findByWorkIdOrderByChapterNumberAsc(workId);

        if (chapters.isEmpty()) {
            return List.of();
        }

        Set<String> docIdSet = new HashSet<>(docIds);
        Set<String> duplicates = new LinkedHashSet<>(); // 순서 유지하면서 중복 제거

        for (Chapter chapter : chapters) {
            // 단일 documentId 체크
            if (chapter.getDocumentId() != null && docIdSet.contains(chapter.getDocumentId())) {
                duplicates.add(chapter.getDocumentId());
            }

            // 병합 배포된 documentIds 배열 체크
            if (chapter.getDocumentIds() != null) {
                for (String existingId : chapter.getDocumentIds()) {
                    if (docIdSet.contains(existingId)) {
                        duplicates.add(existingId);
                    }
                }
            }
        }

        log.info("Found {} duplicate documentIds for workId={}", duplicates.size(), workId);
        return new ArrayList<>(duplicates);
    }
}
