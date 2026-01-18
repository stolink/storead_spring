package com.stolink.backend.domain.stats.service;

import com.stolink.backend.domain.stats.entity.ChapterReadingLog;
import com.stolink.backend.domain.stats.entity.VisitLog;
import com.stolink.backend.domain.stats.repository.ChapterReadingLogRepository;
import com.stolink.backend.domain.stats.repository.VisitLogRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorStatsService {

    private final VisitLogRepository visitLogRepository;
    private final ChapterReadingLogRepository chapterReadingLogRepository;

    // === Logging Methods (Write) ===
    @Transactional
    public void logVisit(UUID workId, UUID userId, String source) {
        // 이미 같은 날 같은 유저가 같은 소스로 방문했는지 체크할 수도 있지만, 일단 단순 insert
        // (단, UV 계산시 DISTINCT 사용하므로 문제 없음)
        VisitLog log = VisitLog.builder()
                .workId(workId)
                .userId(userId)
                .source(source)
                .build();
        visitLogRepository.save(log);
    }

    @Transactional
    public void logChapterRead(UUID workId, UUID chapterId, Integer chapterNumber, UUID userId) {
        // 중복 열람 로그 방지 로직이 필요할 수 있음 (예: 1시간 내 중복 제외). MVP는 단순 insert.
        // 혹은 이미 읽은 챕터인지 ReadingHistory로 체크하지만, Log는 행위 자체를 기록.
        ChapterReadingLog log = ChapterReadingLog.builder()
                .workId(workId)
                .chapterId(chapterId)
                .chapterNumber(chapterNumber)
                .userId(userId)
                .build();
        chapterReadingLogRepository.save(log);
    }

    // === Stats Methods (Read) ===

    public WorkStatsDto getWorkStats(UUID workId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusDays(7);
        LocalDateTime twoWeeksAgo = now.minusDays(14);

        // 1. 누적 독자 수 (전체 기간)
        Long totalReaders = chapterReadingLogRepository.countDistinctUsersByWorkId(workId);

        // 2. 지난주 대비 증가율 (독자 수)
        Long thisWeekNew = chapterReadingLogRepository.countDistinctUsersByWorkIdAndDateRange(workId, oneWeekAgo, now);
        Long lastWeekNew = chapterReadingLogRepository.countDistinctUsersByWorkIdAndDateRange(workId, twoWeeksAgo,
                oneWeekAgo);

        double growthRate = 0.0;
        if (lastWeekNew > 0) {
            growthRate = ((double) (thisWeekNew - lastWeekNew) / lastWeekNew) * 100;
        } else if (thisWeekNew > 0) {
            growthRate = 100.0; // 이전 데이터가 없는데 이번주 유입이 있으면 100% 성장으로 표기 (혹은 N/A)
        }

        // 3. 평균 연독률 (단순화: 1화 독자 수 대비 마지막화 독자 수? 혹은 전체 평균 Retention?)
        // 여기서는 Retention API가 따로 있으므로, 간단히 "1화 대비 평균 유지율" 또는 0으로 둠.
        // TODO: 정교한 평균 연독률 로직 구현 필요. 현재는 Placeholder.
        double avgRetention = 0.0;
        List<ChapterReadingLogRepository.ChapterReaderCount> counts = chapterReadingLogRepository
                .findChapterReaderCounts(workId);
        if (!counts.isEmpty()) {
            // Retention 로직 재활용 가능
            // 예: (전체 챕터 Retention 합 / 챕터 수)
            // 일단 0.0 리턴
        }

        return WorkStatsDto.builder()
                .totalReaders(totalReaders)
                .readerGrowthRate(growthRate)
                .averageRetention(avgRetention)
                .build();
    }

    public List<RetentionDto> getRetentionRates(UUID workId) {
        List<ChapterReadingLogRepository.ChapterReaderCount> counts = chapterReadingLogRepository
                .findChapterReaderCounts(workId);

        if (counts.isEmpty())
            return List.of();

        // 1화 독자 수 (기준)
        long firstChapterCount = counts.stream()
                .filter(c -> c.getChapterNumber() == 1) // 1화가 1부터 시작한다고 가정
                .findFirst()
                .map(ChapterReadingLogRepository.ChapterReaderCount::getReaderCount)
                .orElse(0L);

        if (firstChapterCount == 0)
            return List.of();

        return counts.stream()
                .map(c -> {
                    double rate = ((double) c.getReaderCount() / firstChapterCount) * 100;
                    return RetentionDto.builder()
                            .chapterNumber(c.getChapterNumber())
                            .retentionRate(Math.min(rate, 100.0)) // 100% 초과 방지 (데이터 오차 대비)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<DiscoveryDto> getDiscoverySources(UUID workId) {
        List<VisitLogRepository.SourceCount> sourceCounts = visitLogRepository.findSourceCounts(workId);

        long totalVisits = sourceCounts.stream().mapToLong(VisitLogRepository.SourceCount::getCount).sum();

        return sourceCounts.stream()
                .map(sc -> DiscoveryDto.builder()
                        .source(sc.getSource() == null ? "unknown" : sc.getSource())
                        .count(sc.getCount())
                        .percentage(totalVisits > 0 ? ((double) sc.getCount() / totalVisits) * 100 : 0)
                        .build())
                .collect(Collectors.toList());
    }

    // === DTO Classes ===
    @Getter
    @Builder
    public static class WorkStatsDto {
        private Long totalReaders;
        private Double readerGrowthRate;
        private Double averageRetention;
    }

    @Getter
    @Builder
    public static class RetentionDto {
        private Integer chapterNumber;
        private Double retentionRate;
    }

    @Getter
    @Builder
    public static class DiscoveryDto {
        private String source;
        private Long count;
        private Double percentage;
    }
}
