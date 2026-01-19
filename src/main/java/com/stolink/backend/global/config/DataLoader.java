package com.stolink.backend.global.config;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.user.entity.AuthProvider;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.entity.WorkStatus;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.domain.like.entity.WorkLike;
import com.stolink.backend.domain.like.repository.WorkLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * 프론트엔드 테스트를 위한 데모 데이터 로더
 * - dev 프로필에서만 활성화됩니다.
 * - 애플리케이션 시작 시 자동으로 데모 데이터를 생성합니다.
 */
@Slf4j
@Component
@Profile({ "dev", "local" })
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

        private final UserRepository userRepository;
        private final WorkRepository workRepository;
        private final ChapterRepository chapterRepository;
        private final WorkLikeRepository workLikeRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) {
                log.info("========== 데모 데이터 로더 시작 ==========");

                // 데이터가 100개 미만이면 추가 생성
                long existCount = workRepository.count();
                if (existCount >= 100) {
                        log.info("Works already exist ({} works). Skipping demo data generation.", existCount);
                        return;
                }

                log.info("Current works: {}. Generating dummy data up to 100...", existCount);

                // 1. 테스트 사용자 생성 (기존 사용자 유지)
                List<User> users = createTestUsers();

                // 2. 테스트 작품 생성 (100개까지)
                int countToCreate = 100 - (int) existCount;
                List<Work> works = createDiverseWorks(users, countToCreate);
                log.info("테스트 작품 {}개 추가 생성 완료", works.size());

                // 3. 테스트 챕터 생성
                int totalChapters = createDiverseChapters(works);
                log.info("테스트 챕터 {}개 생성 완료", totalChapters);

                // 4. 테스트 좋아요 생성 (랭킹 데이터용)
                createRandomLikes(works, users);
                log.info("테스트 좋아요 데이터 생성 완료");

                log.info("========== 데모 데이터 로더 완료 ==========");
        }

        private List<Work> createDiverseWorks(List<User> users, int count) {
                List<Work> works = new ArrayList<>();
                Genre[] genres = Genre.values();
                int userCount = users.size();

                String[] prefixes = { "전설의", "어느날 갑자기", "비밀의", "강남의", "이세계", "마지막", "숨겨진", "최강", "평범한", "위험한", "달콤한",
                                "차가운", "붉은", "검은", "푸른", "황금빛" };
                String[] suffixes = { "마법사", "기사", "헌터", "연인", "비밀", "전쟁", "유산", "복수", "성장", "일상", "심판", "이야기", "운명",
                                "재회", "계약", "탈출" };

                for (int i = 0; i < count; i++) {
                        String title = prefixes[i % prefixes.length] + " "
                                        + suffixes[(i / prefixes.length) % suffixes.length] + " " + (i + 1);
                        Genre genre = genres[i % genres.length];
                        User author = users.get(i % userCount);

                        // 다양한 상태 분포: 60% 연재중, 30% 완결, 10% 휴재
                        WorkStatus status;
                        if (i % 10 < 6) {
                                status = WorkStatus.ONGOING;
                        } else if (i % 10 < 9) {
                                status = WorkStatus.COMPLETED;
                        } else {
                                status = WorkStatus.HIATUS;
                        }

                        works.add(Work.builder()
                                        .author(author)
                                        .title(title)
                                        .synopsis(title + "에 대한 흥미진진한 이야기입니다. 이 작품은 " + genre.name()
                                                        + " 장르의 매력을 듬뿍 담고 있으며, 독자 여러분께 새로운 즐거움을 선사할 것입니다. #태그"
                                                        + (i % 10) + " #추천 #꿀잼")
                                        .coverImageUrl("https://picsum.photos/seed/work"
                                                        + (i + (int) workRepository.count()) + "/400/600")
                                        .genre(genre)
                                        .status(status)
                                        .build());
                }

                return workRepository.saveAll(works);
        }

        private int createDiverseChapters(List<Work> works) {
                int totalChapters = 0;
                for (int i = 0; i < works.size(); i++) {
                        Work work = works.get(i);
                        // 각 작품당 1~2화 생성
                        int chapterCount = 1 + (i % 2);

                        // 다양한 유료/무료 패턴
                        // 50% 전체 무료, 30% 1화 무료 + 2화 유료, 20% 전체 유료
                        int pattern = i % 10;
                        boolean isEntirelyFree = (pattern < 5); // 50%
                        boolean isFirstFreeOnly = (pattern >= 5 && pattern < 8); // 30%
                        boolean isAllPaid = (pattern >= 8); // 20%

                        for (int j = 1; j <= chapterCount; j++) {
                                boolean isFree;
                                com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType;
                                int price;

                                if (isEntirelyFree) {
                                        isFree = true;
                                        accessType = com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE;
                                        price = 0;
                                } else if (isFirstFreeOnly) {
                                        isFree = (j == 1); // 1화만 무료
                                        accessType = isFree
                                                        ? com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE
                                                        : com.stolink.backend.domain.chapter.entity.ChapterAccessType.PAID;
                                        price = isFree ? 0 : 10; // 100원 = 10크레딧
                                } else { // isAllPaid
                                        isFree = false;
                                        accessType = com.stolink.backend.domain.chapter.entity.ChapterAccessType.PAID;
                                        price = 15; // 150원 = 15크레딧
                                }

                                chapterRepository.save(Chapter.builder()
                                                .work(work)
                                                .title(work.getTitle() + " - 제" + j + "화")
                                                .content(generateContent(work.getTitle() + "의 " + j
                                                                + "번째 이야기입니다. 신비로운 모험과 긴장감 넘치는 전개가 이어집니다."))
                                                .chapterNumber(j)
                                                .isFree(isFree)
                                                .price(price)
                                                .accessType(accessType)
                                                .build());
                                totalChapters++;
                        }
                }
                return totalChapters;
        }

        /**
         * 테스트 사용자 3명 생성 (이미 존재하면 기존 사용자 반환)
         */
        private List<User> createTestUsers() {
                List<User> users = new ArrayList<>();

                users.add(findOrCreateUser("test1@example.com", "작가김철수", "test1"));
                users.add(findOrCreateUser("test2@example.com", "작가이영희", "test2"));
                users.add(findOrCreateUser("test3@example.com", "작가박민수", "test3"));

                return users;
        }

        /**
         * 이메일로 사용자 조회, 없으면 새로 생성
         */
        private User findOrCreateUser(String email, String nickname, String avatarSeed) {
                return userRepository.findByEmail(email)
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .email(email)
                                                .password(passwordEncoder.encode("password123")) // 암호화된 비밀번호 저장
                                                .nickname(nickname)
                                                .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed="
                                                                + avatarSeed)
                                                .provider(AuthProvider.LOCAL)
                                                .build()));
        }

        /**
         * 짧은 시놉시스를 좀 더 긴 본문으로 확장하는 헬퍼 메서드
         */
        private String generateContent(String synopsis) {
                StringBuilder content = new StringBuilder();
                content.append(synopsis);
                content.append("\n\n");

                // 본문 내용을 좀 더 풍부하게
                content.append("---\n\n");
                content.append("(본문 내용이 계속됩니다...)\n\n");
                content.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ");
                content.append("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ");
                content.append("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.\n\n");
                content.append("다음 화에서 계속...");

                return content.toString();
        }

        private void createRandomLikes(List<Work> works, List<User> users) {
                List<WorkLike> likes = new ArrayList<>();
                // 각 작품마다 랜덤하게 좋아요 생성
                for (Work work : works) {
                        // 작품당 0~3개의 좋아요
                        int likeCount = (int) (Math.random() * 4);
                        for (int i = 0; i < likeCount && i < users.size(); i++) {
                                likes.add(WorkLike.builder()
                                                .work(work)
                                                .user(users.get(i))
                                                .build());
                                // Work의 likeCount도 증가시켜 동기화
                                work.addLike();
                        }
                }
                workLikeRepository.saveAll(likes);
                workRepository.saveAll(works); // likeCount 업데이트
        }
}
