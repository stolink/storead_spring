package com.stolink.backend.domain.payment;

import com.stolink.backend.domain.payment.dto.request.CreditUseRequest;
import com.stolink.backend.domain.payment.entity.Credit;
import com.stolink.backend.domain.payment.repository.CreditRepository;
import com.stolink.backend.domain.payment.service.CreditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.DockerClientFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.testcontainers.junit.jupiter.Testcontainers
@EnabledIf("dockerAvailable")
class CreditConcurrencyTest {

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    @org.testcontainers.junit.jupiter.Container
    static org.testcontainers.containers.PostgreSQLContainer<?> postgres = new org.testcontainers.containers.PostgreSQLContainer<>(
            "postgres:16-alpine");

    @org.springframework.test.context.DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private CreditService creditService;

    @Autowired
    private CreditRepository creditRepository;

    @Test
    @DisplayName("동시성 제어: 100개 스레드 동시 차감 요청 시 데이터 정합성 보장 및 데드락 방지")
    void concurrencyCreditUseTest() throws InterruptedException {
        // given
        UUID userId = UUID.randomUUID();
        long initialBalance = 10000L;
        long deductAmount = 100L;
        int threadCount = 100;

        Credit credit = Credit.createForUser(userId);
        credit.charge(initialBalance);
        creditRepository.save(credit);

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 성공 및 실패 횟수 추적을 위한 원자적 카운터
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger();

        long startTime = System.nanoTime();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    CreditUseRequest request = new CreditUseRequest(deductAmount, "Concurrent Test", null, null);
                    creditService.useCredit(userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("Thread Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        long elapsed = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        System.out.printf("총 소요 시간: %dms, 평균 TPS: %.1f%n", elapsed, threadCount / (elapsed / 1000.0));

        executorService.shutdown();

        // then
        assertThat(completed).as("30초 내 모든 스레드가 완료되어야 한다").isTrue();

        Credit finalCredit = creditRepository.findByUserId(userId).orElseThrow();

        // 1. 잔액 검증
        assertThat(finalCredit.getBalance()).as("잔액은 0원이어야 한다").isEqualTo(0L);

        // 2. 트랜잭션 성공 횟수 검증
        assertThat(successCount.get()).as("100건의 트랜잭션이 모두 성공해야 한다").isEqualTo(threadCount);
        assertThat(failCount.get()).as("실패한 트랜잭션이 없어야 한다").isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 제어: 잔액 부족 시 초과 차감 방지")
    void concurrencyCreditUse_insufficientBalance() throws InterruptedException {
        // given
        UUID userId = UUID.randomUUID();
        // 5000원 보유 -> 100원씩 100번 요청 (50번 성공, 50번 실패 예상)
        Credit credit = Credit.createForUser(userId);
        credit.charge(5000L);
        creditRepository.save(credit);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger();

        long startTime = System.nanoTime();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    creditService.useCredit(userId, new CreditUseRequest(100L, "Test", null, null));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        long elapsed = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        System.out.printf("[Insufficient Balance Test] 소요 시간: %dms, 평균 TPS: %.1f%n", elapsed,
                threadCount / (elapsed / 1000.0));

        executor.shutdown();

        // then
        assertThat(completed).as("30초 내 모든 스레드가 완료되어야 한다").isTrue();

        Credit finalCredit = creditRepository.findByUserId(userId).orElseThrow();
        assertThat(finalCredit.getBalance()).as("잔액은 음수가 되면 안 된다").isGreaterThanOrEqualTo(0L);

        // 5000 / 100 = 50건 성공해야 함
        assertThat(successCount.get()).as("정확히 50건만 성공").isEqualTo(50);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }
}
