# 토스 페이먼츠 결제 시스템 문서

> Spring Boot 3.4.1 기반 크레딧 결제 시스템

## 📌 핵심 기능

- ✅ 토스 페이먼츠 결제 연동 (준비/승인/취소)
- ✅ 크레딧 충전/사용/환불
- ✅ 동시성 제어 (비관적/낙관적 락)
- ✅ 멱등성 보장
- ✅ 웹훅 처리 및 자동 재시도

## 📚 문서 목록

| 문서                                                       | 용도                          |
| ---------------------------------------------------------- | ----------------------------- |
| [API_GUIDE.md](./API_GUIDE.md)                             | REST API 엔드포인트 명세      |
| [TECHNICAL_DOCUMENTATION.md](./TECHNICAL_DOCUMENTATION.md) | 아키텍처, 설계, 구현 상세     |
| [CREDIT_SYSTEM_CHECKLIST.md](./CREDIT_SYSTEM_CHECKLIST.md) | 크레딧 시스템 구현 체크리스트 |

## 🚀 빠른 시작

```bash
# 빌드 및 실행
./gradlew bootRun

# API 테스트
curl http://localhost:8080/api/v1/payments/packages
```

## 🔧 환경 변수

```bash
TOSS_SECRET_KEY=test_sk_zXLkKEypNArWmo50nX3lmeaxYG5R
TOSS_CLIENT_KEY=test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq
```

## 📊 결제 플로우

```
1. POST /payments/prepare (주문 생성)
2. 토스 결제창 호출 (프론트엔드)
3. POST /payments/confirm (결제 승인)
4. 크레딧 자동 지급
```

---

**상세 내용은 각 문서를 참조하세요.**
