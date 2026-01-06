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
        private final PasswordEncoder passwordEncoder;

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
         * 테스트 작품 생성 (각 장르별 5개 이상, 총 25개+)
         * - 기존 작가 3명을 순환하며 할당
         * - 진짜 소설 같은 제목과 시놉시스 사용
         */
        /**
         * 테스트 작품 생성 (각 장르별 6개, 총 30개)
         * - 기존 작가 3명을 순환하며 할당 (모듈러 연산으로 안전한 인덱스 접근)
         * - 진짜 소설 같은 제목과 시놉시스 사용
         * - saveAll()을 사용한 배치 삽입으로 성능 최적화
         */
        private List<Work> createTestWorks(List<User> users) {
                List<Work> works = new ArrayList<>();
                int userCount = users.size();

                // ===================== FANTASY 장르 (6개) =====================
                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("용과 마법사의 전설")
                                .synopsis("평범한 대학생이었던 주인공이 이세계로 소환되어 전설의 용과 함께 마왕을 물리치는 이야기. 치열한 전투와 감동적인 우정, 그리고 성장의 이야기가 펼쳐집니다.")
                                .coverImageUrl("https://picsum.photos/seed/fantasy1/400/600")
                                .genre(Genre.FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("마탑주의 회귀")
                                .synopsis("대마법사로 군림했던 주인공이 의문의 죽음을 맞이하고 100년 전 수련생 시절로 돌아간다. 미래의 기억을 바탕으로 이번 생에서는 다른 선택을 한다.")
                                .coverImageUrl("https://picsum.photos/seed/fantasy2/400/600")
                                .genre(Genre.FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("엘프 왕국의 검은 기사")
                                .synopsis("인간계에서 추방당한 기사가 우연히 엘프 왕국을 발견한다. 두 세계 사이에서 벌어지는 대전쟁, 그리고 금지된 사랑의 이야기.")
                                .coverImageUrl("https://picsum.photos/seed/fantasy3/400/600")
                                .genre(Genre.FANTASY)
                                .status(WorkStatus.COMPLETED)
                                .build());

                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("던전에서 레벨업")
                                .synopsis("갑작스럽게 출현한 던전과 각성자들. 최하위 랭크 각성자였던 주인공이 유일무이한 능력을 얻으며 세계 최강으로 성장해 나가는 이야기.")
                                .coverImageUrl("https://picsum.photos/seed/fantasy4/400/600")
                                .genre(Genre.FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("천년 후의 마법사")
                                .synopsis("마법의 시대가 끝난 천년 후 미래에서 깨어난 고대 마법사. 과학과 마법이 충돌하는 세계에서 새로운 시대를 열어간다.")
                                .coverImageUrl("https://picsum.photos/seed/fantasy5/400/600")
                                .genre(Genre.FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("최강 마법검사")
                                .synopsis("마법과 검술을 동시에 다루는 천재가 세계의 불가사의를 탐험하며 성장하는 이야기. 미지의 던전에서 숨겨진 보물을 찾아나선다.")
                                .coverImageUrl("https://picsum.photos/seed/fantasy6/400/600")
                                .genre(Genre.FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                // ===================== ROMANCE 장르 (6개) =====================
                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("사랑의 계절")
                                .synopsis("봄, 여름, 가을, 겨울... 사계절 속에서 피어나는 두 남녀의 아름다운 사랑 이야기. 운명적인 만남부터 시련을 극복하기까지의 여정을 담았습니다.")
                                .coverImageUrl("https://picsum.photos/seed/romance1/400/600")
                                .genre(Genre.ROMANCE)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("비 오는 날의 카페")
                                .synopsis("작은 동네 카페에서 우연히 마주친 두 사람. 비가 오는 날마다 만나게 되는 그들의 이야기. 서툴지만 진심을 담은 사랑이 시작됩니다.")
                                .coverImageUrl("https://picsum.photos/seed/romance2/400/600")
                                .genre(Genre.ROMANCE)
                                .status(WorkStatus.COMPLETED)
                                .build());

                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("첫사랑의 재회")
                                .synopsis("10년 만에 재회한 첫사랑. 서로 다른 삶을 살아왔지만 여전히 가슴 뛰는 감정은 변하지 않았다. 어른이 되어 다시 시작하는 사랑.")
                                .coverImageUrl("https://picsum.photos/seed/romance3/400/600")
                                .genre(Genre.ROMANCE)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("대기업 CEO와 비서")
                                .synopsis("냉철하기로 유명한 대기업 CEO와 그의 새로운 비서. 업무적인 관계가 점차 달콤한 로맨스로 발전하는 오피스 러브.")
                                .coverImageUrl("https://picsum.photos/seed/romance4/400/600")
                                .genre(Genre.ROMANCE)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("소꿉친구와의 계약 연애")
                                .synopsis("부모님의 성화에 못 이겨 소꿉친구와 가짜 연애를 시작했다. 그런데 가짜였던 감정이 점점 진짜가 되어가는데...")
                                .coverImageUrl("https://picsum.photos/seed/romance5/400/600")
                                .genre(Genre.ROMANCE)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("달빛 아래 프러포즈")
                                .synopsis("여행 중 우연히 만난 두 사람. 짧은 만남이 평생의 인연이 될 줄은 몰랐다. 운명 같은 재회와 로맨틱한 고백의 순간.")
                                .coverImageUrl("https://picsum.photos/seed/romance6/400/600")
                                .genre(Genre.ROMANCE)
                                .status(WorkStatus.COMPLETED)
                                .build());

                // ===================== MARTIAL_ARTS 무협 장르 (6개) =====================
                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("천하제일검")
                                .synopsis("무림맹주의 후계자인 주인공이 복수와 정의 사이에서 갈등하며 천하제일의 검객으로 성장해 나가는 이야기. 화려한 무공과 긴장감 넘치는 대결이 펼쳐집니다.")
                                .coverImageUrl("https://picsum.photos/seed/martial1/400/600")
                                .genre(Genre.MARTIAL_ARTS)
                                .status(WorkStatus.COMPLETED)
                                .build());

                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("철혈무쌍")
                                .synopsis("마교의 성물을 차지하기 위한 정파와 사파의 대격돌. 그 중심에서 양 세력이 모두 두려워하는 한 남자가 있었다.")
                                .coverImageUrl("https://picsum.photos/seed/martial2/400/600")
                                .genre(Genre.MARTIAL_ARTS)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("협객행")
                                .synopsis("부모의 원수를 갚기 위해 15년간 검을 닦아온 청년. 마침내 강호에 발을 내딛는 그의 복수극이 시작된다.")
                                .coverImageUrl("https://picsum.photos/seed/martial3/400/600")
                                .genre(Genre.MARTIAL_ARTS)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("용봉쟁패기")
                                .synopsis("북궁세가의 천재와 남궁세가의 영재. 강호를 양분할 두 천재의 운명적인 대결, 그리고 예상치 못한 우정.")
                                .coverImageUrl("https://picsum.photos/seed/martial4/400/600")
                                .genre(Genre.MARTIAL_ARTS)
                                .status(WorkStatus.COMPLETED)
                                .build());

                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("개방천하")
                                .synopsis("거지 출신으로 무림의 정점에 오른 한 남자의 일대기. 가진 것 없이 시작했지만 천하를 품에 안는다.")
                                .coverImageUrl("https://picsum.photos/seed/martial5/400/600")
                                .genre(Genre.MARTIAL_ARTS)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("비검도")
                                .synopsis("비 오는 날에만 사용할 수 있는 전설의 검법. 그 비기를 전수받은 젊은이가 강호에 새로운 전설을 쓴다.")
                                .coverImageUrl("https://picsum.photos/seed/martial6/400/600")
                                .genre(Genre.MARTIAL_ARTS)
                                .status(WorkStatus.ONGOING)
                                .build());

                // ===================== MODERN_FANTASY 현대판타지 (6개) =====================
                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("서울의 마법사")
                                .synopsis("현대 서울에 숨겨진 마법 사회. 평범한 직장인인 주인공이 우연히 마법 능력을 각성하고 도시의 어둠과 맞서 싸우는 도시 판타지.")
                                .coverImageUrl("https://picsum.photos/seed/modernfantasy1/400/600")
                                .genre(Genre.MODERN_FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("헌터즈 월드")
                                .synopsis("전 세계에 던전이 출현했다. 각성자가 된 평범한 고등학생의 성장 서사. 학교와 던전을 오가며 세계를 구한다.")
                                .coverImageUrl("https://picsum.photos/seed/modernfantasy2/400/600")
                                .genre(Genre.MODERN_FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("망자의 눈")
                                .synopsis("사고 이후 죽은 자들이 보이게 된 청년. 그들의 미련을 풀어주며 현대 도시의 숨겨진 미스터리를 파헤친다.")
                                .coverImageUrl("https://picsum.photos/seed/modernfantasy3/400/600")
                                .genre(Genre.MODERN_FANTASY)
                                .status(WorkStatus.COMPLETED)
                                .build());

                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("강남 뱀파이어")
                                .synopsis("강남 클럽가를 지배하는 뱀파이어 패밀리. 그들 사이에 뛰어든 인간 여대생의 위험하고도 달콤한 밤생활.")
                                .coverImageUrl("https://picsum.photos/seed/modernfantasy4/400/600")
                                .genre(Genre.MODERN_FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("퇴마사 사무소")
                                .synopsis("을지로의 낡은 빌딩에 자리한 비밀 퇴마사 사무소. 도시의 기묘한 사건들을 해결하는 퇴마사들의 일상과 사건 해결기.")
                                .coverImageUrl("https://picsum.photos/seed/modernfantasy5/400/600")
                                .genre(Genre.MODERN_FANTASY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("소서러 인 서울")
                                .synopsis("서울 한복판에서 마법 전쟁이 일어난다. 도시를 지키기 위해 나선 현대 마법사들의 액션 판타지.")
                                .coverImageUrl("https://picsum.photos/seed/modernfantasy6/400/600")
                                .genre(Genre.MODERN_FANTASY)
                                .status(WorkStatus.COMPLETED)
                                .build());

                // ===================== MYSTERY 미스터리 (6개) =====================
                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("밀실의 비밀")
                                .synopsis("밀폐된 저택에서 발생한 불가능 살인. 천재 탐정과 함께 숨겨진 진실을 파헤치는 스릴러 미스터리. 예상치 못한 반전이 독자들을 기다립니다.")
                                .coverImageUrl("https://picsum.photos/seed/mystery1/400/600")
                                .genre(Genre.MYSTERY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("검은 편지")
                                .synopsis("익명의 협박 편지를 받은 사람들이 하나 둘 의문사한다. 과거의 비밀과 현재의 죄가 교차하는 심리 스릴러.")
                                .coverImageUrl("https://picsum.photos/seed/mystery2/400/600")
                                .genre(Genre.MYSTERY)
                                .status(WorkStatus.COMPLETED)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("마지막 증인")
                                .synopsis("교통사고 목격자가 연쇄적으로 사라지고 있다. 마지막 생존 목격자인 주인공의 생존을 건 추격전.")
                                .coverImageUrl("https://picsum.photos/seed/mystery3/400/600")
                                .genre(Genre.MYSTERY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(0 % userCount))
                                .title("새벽 2시의 방문자")
                                .synopsis("매일 새벽 2시, 누군가 문을 두드린다. 하지만 CCTV에는 아무도 찍히지 않는다. 오래된 아파트에 숨겨진 비밀.")
                                .coverImageUrl("https://picsum.photos/seed/mystery4/400/600")
                                .genre(Genre.MYSTERY)
                                .status(WorkStatus.ONGOING)
                                .build());

                works.add(Work.builder()
                                .author(users.get(1 % userCount))
                                .title("쌍둥이 자매의 고백")
                                .synopsis("쌍둥이 자매 중 한 명이 살해당했다. 생존자의 증언과 엇갈리는 증거들. 과연 범인은 누구인가?")
                                .coverImageUrl("https://picsum.photos/seed/mystery5/400/600")
                                .genre(Genre.MYSTERY)
                                .status(WorkStatus.COMPLETED)
                                .build());

                works.add(Work.builder()
                                .author(users.get(2 % userCount))
                                .title("그 해 겨울의 진실")
                                .synopsis("10년 전 겨울, 한 마을에서 일어난 대형 실종 사건. 당시 어린이였던 주인공이 성인이 되어 진실을 파헤치기 시작한다.")
                                .coverImageUrl("https://picsum.photos/seed/mystery6/400/600")
                                .genre(Genre.MYSTERY)
                                .status(WorkStatus.ONGOING)
                                .build());

                // 배치 삽입으로 성능 최적화: 개별 save() 대신 saveAll() 사용
                return workRepository.saveAll(works);
        }

        /**
         * 각 작품당 3~5개의 챕터 생성 (총 30개 작품)
         */
        private int createTestChapters(List<Work> works) {
                int totalChapters = 0;

                // ===================== FANTASY 장르 (0-5) =====================
                // 작품 0: 용과 마법사의 전설
                createChaptersForWork(works.get(0), new String[][] {
                                { "프롤로그: 낯선 세계로",
                                                "눈을 떴을 때, 나는 낯선 숲 속에 있었다. 하늘에는 두 개의 달이 떠 있었고, 공기는 이상하게 달콤한 향기로 가득했다..." },
                                { "1화: 용의 등장", "숲 속을 헤매던 나는 거대한 동굴을 발견했다. 그리고 그 안에서 잠들어 있던 전설의 존재와 마주하게 되었는데..." },
                                { "2화: 첫 번째 시련", "용이 던진 첫 번째 시험. 마법의 기초를 배우기 시작한 나는 예상치 못한 재능을 발견하게 되는데..." }
                });
                totalChapters += 3;

                // 작품 1: 마탑주의 회귀
                createChaptersForWork(works.get(1), new String[][] {
                                { "제1장: 죽음과 재생", "마탑 최상층에서 누군가의 칼에 쓰러졌다. 그리고 눈을 뜨니 100년 전의 내가 되어있었다..." },
                                { "제2장: 수련생 시절", "다시 밟게 된 마탑의 첫 계단. 이번에는 다르게 하겠다. 나를 죽인 자의 정체를 밝힐 것이다..." },
                                { "제3장: 숨겨진 마법", "미래에서만 발견된 마법 공식을 지금 완성할 수 있다. 이것이 나만의 무기가 될 것이다..." }
                });
                totalChapters += 3;

                // 작품 2: 엘프 왕국의 검은 기사
                createChaptersForWork(works.get(2), new String[][] {
                                { "추방자", "인간 왕국에서 쫓겨난 기사. 깊은 숲 속에서 길을 잃은 그는 신비로운 빛을 따라가게 되었다..." },
                                { "은빛 숲의 비밀", "수천 년간 인간에게 숨겨져 왔던 엘프 왕국. 처음 보는 이방인에 대한 경계의 눈초리 속에서..." },
                                { "공주와의 만남", "엘프 왕국의 공주가 숲에서 습격을 당했다. 그녀를 구한 것이 모든 이야기의 시작이었다..." }
                });
                totalChapters += 3;

                // 작품 3: 던전에서 레벨업
                createChaptersForWork(works.get(3), new String[][] {
                                { "E급 각성", "세계에 던전이 출현한 지 10년. 나는 최하위 E급 각성자로 살아가고 있었다..." },
                                { "숨겨진 시스템", "죽음의 문턱에서 나에게만 보이는 시스템 창이 떴다. '플레이어 전용 시스템 활성화'..." },
                                { "혼자만의 레벨업", "혼자서 던전을 공략하며 강해지는 법을 터득해 나간다. 세상은 아직 나의 성장을 모른다..." }
                });
                totalChapters += 3;

                // 작품 4: 천년 후의 마법사
                createChaptersForWork(works.get(4), new String[][] {
                                { "긴 잠에서 깨어나다", "마법의 힘으로 잠들었던 천년의 세월. 눈을 뜬 곳은 콘크리트와 금속으로 뒤덮인 낯선 세계였다..." },
                                { "과학과 마법", "마법은 사라지고 과학이 발전한 세상. 하지만 내 손끝에서 여전히 마나가 흐르고 있다..." },
                                { "숨겨진 마법사들", "이 시대에도 마법사가 있었다. 그들은 정부의 비밀 기관에서 활동하고 있었는데..." }
                });
                totalChapters += 3;

                // 작품 5: 최강 마법검사 (NEW)
                createChaptersForWork(works.get(5), new String[][] {
                                { "첫 던전 탐사", "마법과 검술을 배우기 시작한 첫 해. 스승이 정해준 첫 던전 탐사가 시작되는데..." },
                                { "숨겨진 재능", "마법과 검술을 동시에 구사하는 것은 불가능하다고 했다. 하지만 나는 달랐다..." },
                                { "라이벌의 등장", "왕립 마법검사단의 천재가 나타났다. 그녀와의 대결이 시작되는데..." }
                });
                totalChapters += 3;

                // ===================== ROMANCE 장르 (6-11) =====================
                // 작품 6: 사랑의 계절
                createChaptersForWork(works.get(6), new String[][] {
                                { "봄: 벚꽃이 흩날리던 날", "매년 봄이면 이 카페에서 커피를 마신다. 그날도 평범한 봄날이 될 줄 알았는데, 그녀가 들어왔다..." },
                                { "여름: 해변에서의 재회", "우연이었을까, 운명이었을까. 휴가로 찾은 해변에서 다시 만나게 되었다..." },
                                { "가을: 단풍 아래 고백", "더 이상 숨길 수 없었다. 노랗고 붉은 단풍이 흩날리는 공원에서, 나는 용기를 냈다..." }
                });
                totalChapters += 3;

                // 작품 7: 비 오는 날의 카페
                createChaptersForWork(works.get(7), new String[][] {
                                { "첫 번째 비", "갑자기 내린 비를 피해 작은 카페에 들어왔다. 창가에 앉아 커피를 마시는 그 사람이 눈에 들어왔다..." },
                                { "두 번째 비", "또다시 비가 왔고, 또다시 같은 카페에서 마주쳤다. 우연의 일치일까..." },
                                { "세 번째 비", "이번에는 일부러 비를 기다렸다. 그 사람도 같은 마음이었을까?" }
                });
                totalChapters += 3;

                // 작품 8: 첫사랑의 재회
                createChaptersForWork(works.get(8), new String[][] {
                                { "10년 만의 재회", "동창회 초대장을 받았다. 망설였지만 결국 참석하기로 했다. 그리고 그곳에서 그녀를 다시 만났다..." },
                                { "달라진 것과 변하지 않은 것", "10년이라는 시간은 많은 것을 바꿔 놓았다. 하지만 그녀를 보는 순간, 내 심장은 여전히 같았다..." },
                                { "커피 한 잔의 시간", "연락처를 교환하고 커피를 마시기로 했다. 짧은 시간 안에 10년을 채우려는 우리의 대화..." }
                });
                totalChapters += 3;

                // 작품 9: 대기업 CEO와 비서
                createChaptersForWork(works.get(9), new String[][] {
                                { "첫 출근", "대기업 본사 50층. 냉혹하기로 유명한 CEO의 새 비서로 첫 출근을 했다..." },
                                { "야근의 시작", "업무가 끝나지 않았다. 저녁 10시, 사무실에 남은 건 CEO와 나뿐이었다..." },
                                { "출장", "일주일간의 해외 출장. 호텔에서만 마주치는 상사와 비서의 관계가 미묘하게 변해간다..." }
                });
                totalChapters += 3;

                // 작품 10: 소꿉친구와의 계약 연애
                createChaptersForWork(works.get(10), new String[][] {
                                { "결혼 압박", "명절마다 이어지는 결혼 이야기. 급기야 선 자리까지 잡아버린 부모님..." },
                                { "계약서 작성", "소꿉친구에게 가짜 연인이 되어달라고 부탁했다. 6개월 기한의 계약서까지 작성하면서..." },
                                { "진짜 같은 가짜", "부모님 앞에서 손을 잡고, 볼에 뽀뽀도 했다. 가짜인데... 왜 이렇게 심장이 뛰는 거지?" }
                });
                totalChapters += 3;

                // 작품 11: 달빛 아래 프러포즈 (NEW)
                createChaptersForWork(works.get(11), new String[][] {
                                { "운명의 여행", "혼자 떠난 유럽 여행. 파리의 작은 카페에서 그를 만났다..." },
                                { "짧은 인연", "여행 동안만의 인연이라고 생각했다. 하지만 헤어지는 순간 눈물이 났다..." },
                                { "운명의 재회", "1년 후, 서울에서 그를 다시 만났다. 운명이라는 걸 믿게 되었다..." }
                });
                totalChapters += 3;

                // ===================== MARTIAL_ARTS 무협 장르 (12-17) =====================
                // 작품 12: 천하제일검
                createChaptersForWork(works.get(12), new String[][] {
                                { "제1장: 피 묻은 검", "부모님의 무덤 앞에서 맹세했다. 반드시 원수를 찾아 복수하겠다고..." },
                                { "제2장: 무림맹 입문", "천재라 불리던 어린 시절. 하지만 그것은 시련의 시작에 불과했다..." },
                                { "제3장: 첫 번째 비무", "무림맹 내 비무대회. 숨겨왔던 실력을 드러낼 때가 왔다..." }
                });
                totalChapters += 3;

                // 작품 13: 철혈무쌍
                createChaptersForWork(works.get(13), new String[][] {
                                { "마교의 성물", "수백 년간 봉인되어 있던 마교의 성물이 세상에 나타났다. 정파와 사파가 모두 움직인다..." },
                                { "중원의 폭풍", "한 남자가 있었다. 정파도, 사파도 아닌 그의 출현에 모두가 긴장한다..." },
                                { "피의 결승", "만인의 원수가 된 그가 천하제일을 향해 나아간다. 그의 앞에 서는 자는 모두 쓰러졌다..." }
                });
                totalChapters += 3;

                // 작품 14: 협객행
                createChaptersForWork(works.get(14), new String[][] {
                                { "복수의 칼", "15년, 오직 복수만을 위해 검을 갈았다. 이제 드디어 강호로 나설 때가 왔다..." },
                                { "첫 번째 원수", "원수 명단의 첫 번째 이름을 찾아 길을 나섰다. 그가 숨어 있는 곳은..." },
                                { "강호의 진실", "원수들을 쫓으며 알게 된 충격적인 진실. 부모님의 죽음 뒤에는 더 큰 음모가 있었다..." }
                });
                totalChapters += 3;

                // 작품 15: 용봉쟁패기
                createChaptersForWork(works.get(15), new String[][] {
                                { "북궁의 천재", "북궁세가의 젊은 검객. 태어날 때부터 천재라 불렸다..." },
                                { "남궁의 영재", "남궁세가의 검녀. 북궁의 천재와 쌍벽을 이루는 실력의 소유자..." },
                                { "숙명의 대결", "천하무림대회에서 마침내 맞붙는 두 사람. 승자는 과연?" }
                });
                totalChapters += 3;

                // 작품 16: 개방천하
                createChaptersForWork(works.get(16), new String[][] {
                                { "길 위의 삶", "아버지도, 어머니도 모른다. 거리에서 태어나 거리에서 자랐다..." },
                                { "개방 입문", "세상에서 가장 낮은 곳에 있는 방파, 개방. 거기서 나는 새로운 삶을 시작했다..." },
                                { "장로의 눈", "개방 장로가 나에게 특별한 관심을 보이기 시작했다. 무언가 숨겨진 비밀이..." }
                });
                totalChapters += 3;

                // 작품 17: 비검도 (NEW)
                createChaptersForWork(works.get(17), new String[][] {
                                { "비 오는 날의 전설", "비 오는 날에만 사용할 수 있다는 전설의 검법. 오랫동안 전수자가 없었다..." },
                                { "비의 기운", "비를 다스리는 법을 배우기 시작했다. 하늘과 하나가 되는 느낌..." },
                                { "첫 대결", "비가 내리는 날, 첫 실전이 시작되었다. 나의 검은 비와 함께 춤을 추었다..." }
                });
                totalChapters += 3;

                // ===================== MODERN_FANTASY 현대판타지 (18-23) =====================
                // 작품 18: 서울의 마법사
                createChaptersForWork(works.get(18), new String[][] {
                                { "1화: 각성", "지하철에서 퇴근하던 평범한 일상. 갑자기 눈앞에 펼쳐진 기이한 광경..." },
                                { "2화: 마법 학원", "서울 한복판에 숨겨진 마법사들의 세계. 그곳에서 나는 신입생이 되었다..." },
                                { "3화: 첫 의뢰", "마법사로서의 첫 의뢰. 강남에 출몰하는 괴물을 처리하라..." }
                });
                totalChapters += 3;

                // 작품 19: 헌터즈 월드
                createChaptersForWork(works.get(19), new String[][] {
                                { "던전 출현", "평범한 고등학생이었던 나는, 어느 날 갑자기 몸 안에서 힘이 폭발했다. 각성..." },
                                { "학교생활", "낮에는 평범한 학생, 방과 후에는 던전 사냥꾼. 이중생활이 시작되었다..." },
                                { "첫 던전", "E급 던전에 처음으로 입장했다. 시시할 줄 알았는데 예상과 다르게 위험했다..." }
                });
                totalChapters += 3;

                // 작품 20: 망자의 눈
                createChaptersForWork(works.get(20), new String[][] {
                                { "사고", "교통사고로 일주일간 의식을 잃었다가 깨어났다. 그런데 이상한 것들이 보이기 시작했다..." },
                                { "첫 번째 의뢰", "세상을 떠나지 못한 영혼. 그들의 미련을 풀어주는 것이 이제 나의 일이 되었다..." },
                                { "연쇄 실종", "도시 곳곳에서 사람들이 사라지고 있다. 영혼들의 증언을 통해 진실에 접근하는데..." }
                });
                totalChapters += 3;

                // 작품 21: 강남 뱀파이어
                createChaptersForWork(works.get(21), new String[][] {
                                { "클럽에서의 밤", "친구에게 이끌려 간 강남의 럭셔리 클럽. 그곳에서 이상한 남자들을 만났다..." },
                                { "초대", "그 클럽의 VIP 멤버십 초대권이 내 앞에 놓였다. 호기심에 다시 찾아가게 되는데..." },
                                { "진실", "그들은 인간이 아니었다. 수백 년을 살아온 뱀파이어들. 그리고 나는 이미 너무 많이 알아버렸다..." }
                });
                totalChapters += 3;

                // 작품 22: 퇴마사 사무소
                createChaptersForWork(works.get(22), new String[][] {
                                { "을지로 3가", "낡은 빌딩 지하 1층. '○○ 상담소'라는 간판 뒤에 숨겨진 퇴마사들의 아지트..." },
                                { "첫 의뢰인", "집에 귀신이 붙었다는 의뢰가 들어왔다. 가벼운 일이라고 생각했는데..." },
                                { "강남 빙의 사건", "대기업 임원이 갑자기 미쳐버렸다. 빙의인가, 정신병인가? 조사를 시작하는데..." }
                });
                totalChapters += 3;

                // 작품 23: 소서러 인 서울 (NEW)
                createChaptersForWork(works.get(23), new String[][] {
                                { "마법 전쟁의 서막", "서울 한복판에서 마법 전쟁이 시작되었다. 나는 그 중심에 서게 되었다..." },
                                { "도시를 지켜라", "괴물들이 도시를 습격한다. 마법사들이 하나둘 쓰러지고..." },
                                { "최후의 결전", "서울을 지키기 위한 최후의 결전이 시작된다..." }
                });
                totalChapters += 3;

                // ===================== MYSTERY 미스터리 (24-29) =====================
                // 작품 24: 밀실의 비밀
                createChaptersForWork(works.get(24), new String[][] {
                                { "사건의 시작", "폭풍우 치는 밤, 고립된 저택에서 비명이 울렸다. 밀실에서 발견된 시체..." },
                                { "불가능한 살인", "모든 창문과 문은 잠겨 있었다. 어떻게 범인은 사라질 수 있었을까..." },
                                { "숨겨진 통로", "탐정의 날카로운 눈이 벽난로 뒤의 비밀을 발견했다..." }
                });
                totalChapters += 3;

                // 작품 25: 검은 편지
                createChaptersForWork(works.get(25), new String[][] {
                                { "첫 번째 편지", "검은 봉투에 담긴 협박 편지. '너의 비밀을 알고 있다'라는 한 문장이 적혀 있었다..." },
                                { "첫 번째 희생자", "편지를 받은 사람 중 한 명이 의문사했다. 자살인가, 타살인가?" },
                                { "과거의 그림자", "20년 전의 사건이 현재로 이어진다. 당시 관련자들이 하나씩 연결되기 시작하는데..." }
                });
                totalChapters += 3;

                // 작품 26: 마지막 증인
                createChaptersForWork(works.get(26), new String[][] {
                                { "사고 현장", "평범한 교통사고인 줄 알았다. 하지만 목격자들이 하나씩 사라지고 있다..." },
                                { "도주", "누군가 나를 쫓고 있다. 경찰도 믿을 수 없다. 오직 혼자서 진실을 밝혀야 한다..." },
                                { "추격", "그날 밤 목격한 것의 진실. 그것은 단순한 사고가 아니었다..." }
                });
                totalChapters += 3;

                // 작품 27: 새벽 2시의 방문자
                createChaptersForWork(works.get(27), new String[][] {
                                { "노크 소리", "매일 밤 새벽 2시 정각. 누군가 현관문을 두드린다. 하지만 아무도 없다..." },
                                { "CCTV", "경비실에 CCTV 확인을 부탁했다. 아무것도 찍혀있지 않았다..." },
                                { "이전 거주자", "이 집에 전에 살던 사람에 대해 조사하기 시작했다. 충격적인 과거가 드러나는데..." }
                });
                totalChapters += 3;

                // 작품 28: 쌍둥이 자매의 고백
                createChaptersForWork(works.get(28), new String[][] {
                                { "사건", "쌍둥이 자매 중 언니가 살해당했다. 유일한 증인은 동생뿐이다..." },
                                { "증언", "동생의 증언과 물적 증거가 맞지 않는다. 무언가를 숨기고 있는 것 같다..." },
                                { "쌍둥이의 비밀", "그들에게는 아무도 모르는 비밀이 있었다. 그날 밤의 진실은..." }
                });
                totalChapters += 3;

                // 작품 29: 그 해 겨울의 진실 (NEW)
                createChaptersForWork(works.get(29), new String[][] {
                                { "실종 사건", "10년 전 겨울, 작은 마을에서 아이들이 사라졌다. 나도 그 중 한 명이었다..." },
                                { "기억의 파편", "사라졌던 기억이 조금씩 되살아난다. 그날 밤 무슨 일이 있었던 걸까..." },
                                { "진실을 향해", "마을로 돌아와 진실을 파헤치기 시작한다. 하지만 누군가가 방해하고 있다..." }
                });
                totalChapters += 3;

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
