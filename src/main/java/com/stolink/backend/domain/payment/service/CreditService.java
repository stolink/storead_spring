package com.stolink.backend.domain.payment.service;

import com.stolink.backend.domain.payment.dto.request.CreditUseRequest;
import com.stolink.backend.domain.payment.dto.response.CreditResponse;
import com.stolink.backend.domain.payment.dto.response.CreditTransactionResponse;
import com.stolink.backend.domain.payment.entity.Credit;
import com.stolink.backend.domain.payment.entity.CreditTransaction;
import com.stolink.backend.domain.payment.entity.CreditTransactionType;
import com.stolink.backend.domain.payment.exception.PaymentExceptions;
import com.stolink.backend.domain.payment.repository.CreditRepository;
import com.stolink.backend.domain.payment.repository.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {

    private final CreditRepository creditRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    /**
     * 크레딧 잔액 조회 (없으면 자동 생성)
     */
    @Transactional
    public CreditResponse getCredit(UUID userId) {
        Credit credit = creditRepository.findByUserId(userId)
            .orElseGet(() -> {
                Credit newCredit = Credit.createForUser(userId);
                return creditRepository.save(newCredit);
            });
        return CreditResponse.from(credit);
    }

    /**
     * 크레딧 사용
     */
    @Transactional
    public CreditResponse useCredit(UUID userId, CreditUseRequest request) {
        Credit credit = creditRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new PaymentExceptions.CreditNotFoundException("크레딧 정보를 찾을 수 없습니다."));

        Long balanceBefore = credit.getBalance();

        credit.use(request.amount());
        creditRepository.save(credit);

        CreditTransaction transaction = CreditTransaction.createUseTransaction(
            userId,
            credit.getId(),
            request.amount(),
            balanceBefore,
            request.description(),
            request.referenceType(),
            request.referenceId()
        );
        creditTransactionRepository.save(transaction);

        log.info("크레딧 사용: userId={}, amount={}, balance={}",
            userId, request.amount(), credit.getBalance());

        return CreditResponse.from(credit);
    }

    /**
     * 크레딧 사용 가능 여부 확인 (간단)
     */
    @Transactional(readOnly = true)
    public boolean canUseCredit(UUID userId, Long amount) {
        return creditRepository.findByUserId(userId)
            .map(credit -> credit.canUse(amount))
            .orElse(false);
    }

    /**
     * 크레딧 사용 가능 여부 확인 (상세)
     */
    @Transactional(readOnly = true)
    public com.stolink.backend.domain.payment.dto.response.CreditCheckResponse checkCredit(UUID userId, Long amount) {
        Credit credit = creditRepository.findByUserId(userId)
            .orElseGet(() -> Credit.createForUser(userId));

        Long currentBalance = credit.getBalance();

        if (credit.canUse(amount)) {
            return com.stolink.backend.domain.payment.dto.response.CreditCheckResponse.available(currentBalance, amount);
        } else {
            return com.stolink.backend.domain.payment.dto.response.CreditCheckResponse.insufficient(currentBalance, amount);
        }
    }

    /**
     * 크레딧 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<CreditTransactionResponse> getTransactions(UUID userId, Pageable pageable) {
        return creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(CreditTransactionResponse::from);
    }

    /**
     * 특정 타입의 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<CreditTransactionResponse> getTransactionsByType(
            UUID userId, CreditTransactionType type, Pageable pageable) {
        return creditTransactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable)
            .map(CreditTransactionResponse::from);
    }
}
