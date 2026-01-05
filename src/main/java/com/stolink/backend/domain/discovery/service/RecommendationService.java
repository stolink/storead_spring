package com.stolink.backend.domain.discovery.service;

import com.stolink.backend.domain.discovery.dto.ContinueReadingResponse;
import com.stolink.backend.domain.discovery.dto.DiscoveryWorkResponse;
import com.stolink.backend.domain.user.entity.ReadingHistory;
import com.stolink.backend.domain.user.repository.ReadingHistoryRepository;
import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

        private final ReadingHistoryRepository readingHistoryRepository;
        private final WorkRepository workRepository;

        /**
         * 읽던 작품 목록 조회
         */
        public List<ContinueReadingResponse> getContinueReading(UUID userId) {
                Pageable limit = PageRequest.of(0, 10); // 최근 10개
                List<ReadingHistory> histories = readingHistoryRepository.findRecentByUserId(userId, limit);

                if (histories.isEmpty()) {
                        return Collections.emptyList();
                }

                // Work 정보 조회
                List<UUID> workIds = histories.stream()
                                .map(ReadingHistory::getWorkId)
                                .collect(Collectors.toList());

                Map<UUID, Work> workMap = workRepository.findAllById(workIds).stream()
                                .collect(Collectors.toMap(Work::getId, w -> w));

                return histories.stream()
                                .filter(h -> workMap.containsKey(h.getWorkId()))
                                .map(h -> ContinueReadingResponse.of(h, workMap.get(h.getWorkId())))
                                .collect(Collectors.toList());
        }

        /**
         * 취향 저격 추천 작품 조회 (태그/장르 기반)
         */
        public List<DiscoveryWorkResponse> getTagBasedRecommendations(UUID userId) {
                // 1. 사용자가 최근에 읽은 작품들의 장르 분석
                List<ReadingHistory> histories = readingHistoryRepository.findRecentByUserId(userId,
                                PageRequest.of(0, 20));

                if (histories.isEmpty()) {
                        // 읽은 기록이 없으면 전체 인기작 반환
                        return workRepository.findAll(PageRequest.of(0, 10)).stream() // 실제로는 인기순 정렬 필요
                                        .map(DiscoveryWorkResponse::from)
                                        .collect(Collectors.toList());
                }

                List<UUID> readWorkIds = histories.stream()
                                .map(ReadingHistory::getWorkId)
                                .collect(Collectors.toList());

                Map<UUID, Work> readWorks = workRepository.findAllById(readWorkIds).stream()
                                .collect(Collectors.toMap(Work::getId, w -> w));

                // 가장 많이 본 장르 추출
                Map<Genre, Long> genreCounts = histories.stream()
                                .filter(h -> readWorks.containsKey(h.getWorkId()))
                                .map(h -> readWorks.get(h.getWorkId()).getGenre())
                                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

                List<Genre> topGenres = genreCounts.entrySet().stream()
                                .sorted(Map.Entry.<Genre, Long>comparingByValue().reversed())
                                .limit(2)
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toList());

                if (topGenres.isEmpty()) {
                        return Collections.emptyList();
                }

                // 2. 해당 장르의 작품 추천 (이미 읽은 작품 제외)
                // 2. 해당 장르의 작품 추천 (이미 읽은 작품 제외)
                // DB 레벨에서 필터링하여 정확한 페이지네이션 보장
                return workRepository.findByGenreAndIdNotIn(topGenres.get(0), readWorkIds, PageRequest.of(0, 10))
                                .stream()
                                .map(DiscoveryWorkResponse::from)
                                .collect(Collectors.toList());
        }
}
