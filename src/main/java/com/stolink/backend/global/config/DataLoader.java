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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

        @Override
        @Transactional
        public void run(String... args) {
                log.info("========== 데모 데이터 로더 시작 ==========");

                // 이미 데이터가 있으면 스킵
                // 이미 데이터가 있으면 스킵 (작품 기준)
                if (workRepository.count() > 0) {
                        log.info("Works already exist. Skipping demo data generation.");
                        return;
                }

                // 1. 테스트 사용자 생성
                List<User> users = createTestUsers();
                log.info("테스트 사용자 {}명 생성 완료", users.size());

                // 2. 테스트 작품 생성
                List<Work> works = createTestWorks(users);
                log.info("테스트 작품 {}개 생성 완료", works.size());

                // 3. 테스트 챕터 생성
                int totalChapters = createTestChapters(works);
                log.info("테스트 챕터 {}개 생성 완료", totalChapters);

                log.info("========== 데모 데이터 로더 완료 ==========");
        }

        /**
         * 테스트 사용자 3명 생성
         */
        private List<User> createTestUsers() {
                List<User> users = new ArrayList<>();

                users.add(userRepository.save(User.builder()
                                .email("test1@example.com")
                                .password("password123") // 데모용 평문 비밀번호
                                .nickname("작가김철수")
                                .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=test1")
                                .provider(AuthProvider.LOCAL)
                                .build()));

                users.add(userRepository.save(User.builder()
                                .email("test2@example.com")
                                .password("password123") // 데모용 평문 비밀번호
                                .nickname("작가이영희")
                                .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=test2")
                                .provider(AuthProvider.LOCAL)
                                .build()));

                users.add(userRepository.save(User.builder()
                                .email("test3@example.com")
                                .password("password123") // 데모용 평문 비밀번호
                                .nickname("작가박민수")
                                .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=test3")
                                .provider(AuthProvider.LOCAL)
                                .build()));

                return users;
        }

        /**
         * 테스트 작품 5개 생성 (다양한 장르)
         */
        private List<Work> createTestWorks(List<User> users) {
                List<Work> works = new ArrayList<>();

                // 작품 1: 판타지
                works.add(workRepository.save(Work.builder()
                                .author(users.get(0))
                                .title("용과 마법사의 전설")
                                .synopsis("평범한 대학생이었던 주인공이 이세계로 소환되어 전설의 용과 함께 마왕을 물리치는 이야기. 치열한 전투와 감동적인 우정, 그리고 성장의 이야기가 펼쳐집니다.")
                                .coverImageUrl("https://picsum.photos/seed/fantasy1/400/600")
                                .genre(Genre.FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build()));

                // 작품 2: 로맨스
                works.add(workRepository.save(Work.builder()
                                .author(users.get(1))
                                .title("사랑의 계절")
                                .synopsis("봄, 여름, 가을, 겨울... 사계절 속에서 피어나는 두 남녀의 아름다운 사랑 이야기. 운명적인 만남부터 시련을 극복하기까지의 여정을 담았습니다.")
                                .coverImageUrl("https://picsum.photos/seed/romance1/400/600")
                                .genre(Genre.ROMANCE)
                                .status(WorkStatus.ONGOING)
                                .build()));

                // 작품 3: 무협
                works.add(workRepository.save(Work.builder()
                                .author(users.get(2))
                                .title("천하제일검")
                                .synopsis("무림맹주의 후계자인 주인공이 복수와 정의 사이에서 갈등하며 천하제일의 검객으로 성장해 나가는 이야기. 화려한 무공과 긴장감 넘치는 대결이 펼쳐집니다.")
                                .coverImageUrl("https://picsum.photos/seed/martial1/400/600")
                                .genre(Genre.MARTIAL_ARTS)
                                .status(WorkStatus.COMPLETED)
                                .build()));

                // 작품 4: 미스터리
                works.add(workRepository.save(Work.builder()
                                .author(users.get(0))
                                .title("밀실의 비밀")
                                .synopsis("밀폐된 저택에서 발생한 불가능 살인. 천재 탐정과 함께 숨겨진 진실을 파헤치는 스릴러 미스터리. 예상치 못한 반전이 독자들을 기다립니다.")
                                .coverImageUrl("https://picsum.photos/seed/mystery1/400/600")
                                .genre(Genre.MYSTERY)
                                .status(WorkStatus.ONGOING)
                                .build()));

                // 작품 5: 현대판타지
                works.add(workRepository.save(Work.builder()
                                .author(users.get(1))
                                .title("서울의 마법사")
                                .synopsis("현대 서울에 숨겨진 마법 사회. 평범한 직장인인 주인공이 우연히 마법 능력을 각성하고 도시의 어둠과 맞서 싸우는 도시 판타지.")
                                .coverImageUrl("https://picsum.photos/seed/modernfantasy1/400/600")
                                .genre(Genre.MODERN_FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build()));

                return works;
        }

        /**
         * 각 작품당 3~5개의 챕터 생성
         */
        private int createTestChapters(List<Work> works) {
                int totalChapters = 0;

                // 작품 1 챕터들
                createChaptersForWork(works.get(0), new String[][] {
                                { "프롤로그: 낯선 세계로",
                                                "눈을 떴을 때, 나는 낯선 숲 속에 있었다. 하늘에는 두 개의 달이 떠 있었고, 공기는 이상하게 달콤한 향기로 가득했다..." },
                                { "1화: 용의 등장", "숲 속을 헤매던 나는 거대한 동굴을 발견했다. 그리고 그 안에서 잠들어 있던 전설의 존재와 마주하게 되었는데..." },
                                { "2화: 첫 번째 시련", "용이 던진 첫 번째 시험. 마법의 기초를 배우기 시작한 나는 예상치 못한 재능을 발견하게 되는데..." },
                                { "3화: 동료들", "마을에서 만난 새로운 동료들. 검사 민수, 힐러 영희, 그리고 수수께끼의 마법사..." },
                                { "4화: 어둠의 기운", "마왕의 부하들이 마을을 습격했다. 첫 번째 진짜 전투가 시작되는데..." }
                });
                totalChapters += 5;

                // 작품 2 챕터들
                createChaptersForWork(works.get(1), new String[][] {
                                { "봄: 벚꽃이 흩날리던 날", "매년 봄이면 이 카페에서 커피를 마신다. 그날도 평범한 봄날이 될 줄 알았는데, 그녀가 들어왔다..." },
                                { "여름: 해변에서의 재회", "우연이었을까, 운명이었을까. 휴가로 찾은 해변에서 다시 만나게 되었다..." },
                                { "가을: 단풍 아래 고백", "더 이상 숨길 수 없었다. 노랗고 붉은 단풍이 흩날리는 공원에서, 나는 용기를 냈다..." }
                });
                totalChapters += 3;

                // 작품 3 챕터들
                createChaptersForWork(works.get(2), new String[][] {
                                { "제1장: 피 묻은 검", "부모님의 무덤 앞에서 맹세했다. 반드시 원수를 찾아 복수하겠다고..." },
                                { "제2장: 무림맹 입문", "천재라 불리던 어린 시절. 하지만 그것은 시련의 시작에 불과했다..." },
                                { "제3장: 첫 번째 비무", "무림맹 내 비무대회. 숨겨왔던 실력을 드러낼 때가 왔다..." },
                                { "제4장: 그림자 속 음모", "무림맹 내부에 숨어있는 배신자. 아버지를 죽인 원수의 실체가 드러나기 시작하는데..." }
                });
                totalChapters += 4;

                // 작품 4 챕터들
                createChaptersForWork(works.get(3), new String[][] {
                                { "사건의 시작", "폭풍우 치는 밤, 고립된 저택에서 비명이 울렸다. 밀실에서 발견된 시체..." },
                                { "불가능한 살인", "모든 창문과 문은 잠겨 있었다. 어떻게 범인은 사라질 수 있었을까..." },
                                { "숨겨진 통로", "탐정의 날카로운 눈이 벽난로 뒤의 비밀을 발견했다..." }
                });
                totalChapters += 3;

                // 작품 5 챕터들
                createChaptersForWork(works.get(4), new String[][] {
                                { "1화: 각성", "지하철에서 퇴근하던 평범한 일상. 갑자기 눈앞에 펼쳐진 기이한 광경..." },
                                { "2화: 마법 학원", "서울 한복판에 숨겨진 마법사들의 세계. 그곳에서 나는 신입생이 되었다..." },
                                { "3화: 첫 의뢰", "마법사로서의 첫 의뢰. 강남에 출몰하는 괴물을 처리하라..." },
                                { "4화: 어둠의 조직", "도시의 어둠 속에 숨어있는 사악한 마법사 조직의 실체가 드러나기 시작하는데..." }
                });
                totalChapters += 4;

                return totalChapters;
        }

        /**
         * 특정 작품에 대한 챕터들을 생성하는 헬퍼 메서드
         */
        private void createChaptersForWork(Work work, String[][] chaptersData) {
                for (int i = 0; i < chaptersData.length; i++) {
                        chapterRepository.save(Chapter.builder()
                                        .work(work)
                                        .title(chaptersData[i][0])
                                        .content(generateContent(chaptersData[i][1]))
                                        .chapterNumber(i + 1)
                                        .build());
                }
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
}
