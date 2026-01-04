package com.stolink.backend.domain.payment.service;

import com.stolink.backend.domain.payment.dto.response.CreditPackageResponse;
import com.stolink.backend.domain.payment.exception.PaymentExceptions;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CreditPackageService {

    private static final List<CreditPackage> PACKAGES = Arrays.asList(
        new CreditPackage(1L, "크레딧 100점", 1000L, 100L, 0L, false),
        new CreditPackage(2L, "크레딧 500점 (+50 보너스)", 5000L, 500L, 50L, true),
        new CreditPackage(3L, "크레딧 1000점 (+150 보너스)", 10000L, 1000L, 150L, false),
        new CreditPackage(4L, "크레딧 5000점 (+1000 보너스)", 50000L, 5000L, 1000L, false)
    );

    /**
     * 모든 크레딧 패키지 조회
     */
    public List<CreditPackageResponse> getAllPackages() {
        return PACKAGES.stream()
            .map(pkg -> new CreditPackageResponse(
                pkg.getId(),
                pkg.getName(),
                pkg.getPrice(),
                pkg.getCreditAmount(),
                pkg.getBonusCredit(),
                pkg.isPopular()
            ))
            .toList();
    }

    /**
     * ID로 크레딧 패키지 조회
     */
    public CreditPackage getPackage(Long packageId) {
        return PACKAGES.stream()
            .filter(pkg -> pkg.getId().equals(packageId))
            .findFirst()
            .orElseThrow(() -> new PaymentExceptions.CreditPackageNotFoundException(
                "크레딧 패키지를 찾을 수 없습니다: " + packageId
            ));
    }
}
