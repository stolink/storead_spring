package com.stolink.backend.domain.payment;

import com.stolink.backend.domain.payment.dto.request.PaymentPrepareRequest;
import com.stolink.backend.domain.payment.entity.Payment;
import com.stolink.backend.domain.payment.exception.PaymentExceptions;
import com.stolink.backend.domain.payment.repository.PaymentRepository;
import com.stolink.backend.domain.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결제 멱등성(Idempotency) 통합 테스트
 *
 * Testcontainers를 사용하여 실제 PostgreSQL 환경에서
 * 100개의 동시 요청에 대한 중복 결제 방지를 검증합니다.
 *
 * 테스트 유형: 통합 테스트 (Integration Test)
 * - 실제 Docker PostgreSQL 컨테이너 사용
 * - 실제 DB 제약 조건(UNIQUE) + 비관적 락 검증
 * - Docker가 없는 환경(CI 전)에서는 자동으로 스킵됩니다.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("dockerAvailable")
class PaymentIdempotencyIntegrationTest {

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("storead_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("동일 멱등성 키로 100개 동시 요청 시 단 1건만 저장된다")
    void preparePayment_WhenSameIdempotencyKey_100ConcurrentRequests_ShouldSaveOnlyOne() throws InterruptedException {
        // given
        UUID userId = UUID.randomUUID();
        PaymentPrepareRequest request = new PaymentPrepareRequest(1L);
        String sharedIdempotencyKey = "idem-" + UUID.randomUUID();

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        // when: 100개의 스레드가 동시에 같은 멱등성 키로 결제 준비 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentService.preparePayment(userId, request, sharedIdempotencyKey);
                    successCount.incrementAndGet();
                } catch (PaymentExceptions.DuplicatePaymentException e) {
                    duplicateCount.incrementAndGet();
                } catch (Exception e) {
                    unexpectedErrors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(unexpectedErrors)
                .withFailMessage("예상치 못한 예외 발생: %s", unexpectedErrors)
                .isEmpty();

        assertThat(successCount.get())
                .withFailMessage("성공한 요청이 1건이어야 하지만 %d건이 성공했습니다.", successCount.get())
                .isEqualTo(1);

        assertThat(successCount.get() + duplicateCount.get())
                .isEqualTo(threadCount);

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments)
                .withFailMessage("DB에 저장된 결제 건수가 1건이어야 하지만 %d건입니다.", payments.size())
                .hasSize(1);
    }

    @Test
    @DisplayName("서로 다른 멱등성 키로 100개 동시 요청 시 모두 독립적으로 저장된다")
    void preparePayment_WhenDifferentIdempotencyKeys_100ConcurrentRequests_ShouldSaveAll() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        // when: 100개의 스레드가 각자 다른 userId, 다른 멱등성 키로 요청
        for (int i = 0; i < threadCount; i++) {
            UUID userId = UUID.randomUUID();
            String uniqueIdempotencyKey = "idem-" + UUID.randomUUID(); // 각자 고유 키
            PaymentPrepareRequest request = new PaymentPrepareRequest(1L);

            executor.submit(() -> {
                try {
                    paymentService.preparePayment(userId, request, uniqueIdempotencyKey);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(paymentRepository.count()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("클라이언트 미제공 시 서버가 자동 생성한 멱등성 키도 중복 방지가 동작한다")
    void preparePayment_WhenNoClientIdempotencyKey_ServerGeneratesUniqueKeys() throws InterruptedException {
        // given
        int threadCount = 50;
        UUID userId = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when: 클라이언트 멱등성 키 없이(null) 요청 → 서버가 orderId 기반으로 자동 생성
        for (int i = 0; i < threadCount; i++) {
            PaymentPrepareRequest request = new PaymentPrepareRequest(1L);
            executor.submit(() -> {
                try {
                    paymentService.preparePayment(userId, request, null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 중복이든 다른 오류든 카운트 제외
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then: orderId 기반으로 생성되므로 각 요청마다 다른 키 → 모두 성공
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(paymentRepository.count()).isEqualTo(threadCount);
    }
}
