# AI Architecture - Crawling to AI Jobs

이 문서는 FinSight 백엔드의 **뉴스 크롤링 → AI 처리 파이프라인** 아키텍처를 정리한 것입니다.

---

## 분석 기준

| 항목 | 값 |
|------|-----|
| 브랜치 | `refactor/#24-optimization` |
| 커밋 | `fe2afdc65f41a845f4fcf801863add1c086fa1a6` |
| 상태 | Clean (working tree clean) |
| 분석일 | 2026-01-19 |

---

## 전체 개요

### 핵심 흐름 요약

1. **크롤링**: 네이버 경제 뉴스 8개 섹션을 주기적으로 크롤링하여 DB에 저장
2. **AI Job 생성**: 저장된 기사에 대해 SUMMARY Job을 큐에 등록
3. **AI 처리**: Worker가 PENDING Job을 처리 (OpenAI API 호출)
4. **후속 작업**: SUMMARY 완료 후 TERM_CARDS, INSIGHT, QUIZ 등 후속 Job 생성
5. **장애 복구**: Sweeper가 stuck Job과 재시도 대기 Job을 자동 복구

---

## 컴포넌트 맵

### 크롤링 관련

| 패키지/클래스 | 역할 |
|--------------|------|
| `naver.application.usecase.NaverEconomyCrawlScheduler` | Cron 기반 스케줄러 (5분 주기) |
| `naver.domain.service.NaverCrawlerService` | 크롤링 로직 (섹션별 트랜잭션 분리) |
| `naver.persistence.entity.NaverArticleEntity` | 기사 엔티티 (oid, aid 유니크) |
| `naver.domain.constant.NaverEconomySection` | 8개 경제 섹션 enum |

### AI Job 관련

| 패키지/클래스 | 역할 |
|--------------|------|
| `ai.domain.service.AiJobService` | Job 생명주기 관리 (enqueue, process, 상태 전이) |
| `ai.domain.worker.AiJobWorker` | PENDING Job 처리 (30초 주기) |
| `ai.domain.worker.AiJobSweeper` | Stuck/RetryWait Job 복구 (1분 주기) |
| `ai.domain.lock.AiJobLockService` | Redis 분산락 (중복 실행 방지) |
| `ai.persistence.entity.AiJobEntity` | Job 엔티티 (상태, 재시도, 에러 정보) |
| `ai.persistence.entity.AiJobType` | Job 유형 enum (SUMMARY, TERM_CARDS 등) |
| `ai.persistence.entity.AiJobStatus` | Job 상태 enum |

### OpenAI 연동

| 패키지/클래스 | 역할 |
|--------------|------|
| `ai.domain.client.OpenAiClient` | OpenAI API 호출 (JSON Schema 모드) |
| `ai.exception.code.AiErrorCode` | 에러 코드 정의 (suspendable, retryable 분류) |

### 메트릭/로깅

| 패키지/클래스 | 역할 |
|--------------|------|
| `ai.domain.metrics.AiMetrics` | Micrometer 기반 메트릭 (enqueue, process, sweeper) |

---

## 시퀀스 다이어그램

### 크롤링 → AI Job 생성

```
1. NaverEconomyCrawlScheduler.run() [Cron 5분 주기]
   ↓
2. NaverCrawlerService.crawlAllOnce()
   ↓
3. for each 섹션 (금융, 증권, 산업/재계, ...) [트랜잭션 분리]
   ↓
4. crawlSection(section)
   - 목록 페이지 크롤링 (.section_latest 영역)
   - 기사 상세 크롤링 (제목, 본문, 발행일)
   - NaverArticleEntity 저장
   ↓
5. 본문 길이 >= minContentLengthForAi 이면
   ↓
6. AiJobService.enqueueSummary(article)
   - AiJobEntity(PENDING, SUMMARY) 생성
```

### AI Job 처리

```
1. AiJobWorker.runScheduled() [Cron 30초 주기]
   ↓
2. for each JobType (SUMMARY → TERM_CARDS → INSIGHT → QUIZ_CONTENT → QUIZ_TERM)
   ↓
3. findPendingJobIds(type, batchSize)
   ↓
4. for each jobId
   ↓
5. AiJobLockService.tryLock(jobId) [Redis SET NX]
   ↓
6. AiJobService.tryMarkRunning(jobId) [PENDING → RUNNING]
   ↓
7. process[Type](jobId)
   - OpenAiClient.createJsonSchemaResponse()
   - 결과 파싱 및 저장
   ↓
8. 성공: complete[Type]Success() → 후속 Job enqueue
   실패: handleJobError() → 상태 전이
   ↓
9. AiJobLockService.unlock(jobId)
```

### Sweeper 복구

```
1. AiJobSweeper.sweep() [Cron 1분 주기]
   ↓
2. recoverStuckJobs()
   - RUNNING + runningStartedAt > 10분 전
   - canRetry() → RETRY_WAIT (1분 후 재시도)
   - 재시도 불가 → FAILED
   ↓
3. recoverRetryWaitJobs()
   - RETRY_WAIT + nextRunAt <= now
   - PENDING으로 전환
```

---

## Job 상태 전이표

### 상태 전이

| 현재 상태 | 이벤트 | 다음 상태 | 설명 |
|----------|--------|----------|------|
| (없음) | enqueue | PENDING | Job 생성 |
| PENDING | tryMarkRunning() | RUNNING | Worker가 처리 시작 |
| RUNNING | 처리 성공 | SUCCESS | 완료 |
| RUNNING | suspendable 에러 | SUSPENDED | 쿼터 소진, 인증 실패 등 |
| RUNNING | retryable 에러 + canRetry | RETRY_WAIT | 429, 5xx, timeout |
| RUNNING | 기타 에러 or 재시도 초과 | FAILED | 최종 실패 |
| RUNNING | stuck (10분 초과) + canRetry | RETRY_WAIT | Sweeper 복구 |
| RUNNING | stuck (10분 초과) + 재시도 불가 | FAILED | Sweeper 실패 처리 |
| RETRY_WAIT | nextRunAt 도래 | PENDING | Sweeper가 전환 |
| SUSPENDED | 관리자 resume API | PENDING | 수동 재개 |

### 상태 설명

| 상태 | 설명 |
|------|------|
| PENDING | 처리 대기 중 |
| RUNNING | Worker가 처리 중 |
| SUCCESS | 처리 완료 |
| FAILED | 최종 실패 (재시도 불가) |
| RETRY_WAIT | 재시도 대기 (nextRunAt 이후 PENDING으로) |
| SUSPENDED | 수동 확인 필요 (쿼터 소진, 인증 실패 등) |

---

## 장애 시나리오 처리

### 1. 워커 중단 (서버 재시작)

| 상황 | 처리 |
|------|------|
| PENDING 상태 Job | 재시작 후 Worker가 정상 처리 |
| RUNNING 상태 Job | Sweeper가 10분 후 RETRY_WAIT로 전환 |

**빠른 복구 방법**: DB에서 `UPDATE ai_jobs SET status='PENDING' WHERE status='RUNNING'`

### 2. OpenAI Quota 소진

| 에러 코드 | 처리 |
|----------|------|
| `insufficient_quota` | SUSPENDED 전환 |
| `insufficient_balance` | SUSPENDED 전환 |

**복구**: 결제 후 Admin API `/api/v1/admin/ai/jobs/resume?reason=QUOTA` 호출

### 3. Rate Limit (429)

| 상황 | 처리 |
|------|------|
| 첫 번째 429 | RETRY_WAIT (30초 후 재시도) |
| 연속 429 | 지수 백오프 (30초 × 2^retryCount) |
| 재시도 초과 | FAILED |

### 4. Timeout

| 상황 | 처리 |
|------|------|
| API 응답 20초 초과 | retryable로 분류 → RETRY_WAIT |
| 연속 timeout | 지수 백오프 후 FAILED |

### 5. 잘못된 요청 (400)

| 상황 | 처리 |
|------|------|
| 프롬프트/스키마 오류 | FAILED (재시도 불가) |

---

## 메트릭 포인트

### 크롤링

| 메트릭명 | 태그 | 설명 |
|---------|------|------|
| `crawler_articles_total` | section, status | 섹션별/상태별 기사 수 |
| `crawler_events_total` | scope, status | 전체 이벤트 |
| `crawler_run_seconds` | scope | 크롤링 소요 시간 |
| `crawler_fetch_seconds` | type | HTTP 요청 시간 |

### AI Job

| 메트릭명 | 태그 | 설명 |
|---------|------|------|
| `ai_job_enqueue_total` | type, result | Job enqueue 통계 |
| `ai_job_processed_total` | type, result | Job 처리 결과 |
| `ai_worker_events_total` | event | Worker 실행 이벤트 |
| `ai_worker_job_seconds` | type | Job 소요 시간 |
| `ai_sweeper_events_total` | event | Sweeper 실행 이벤트 |
| `ai_sweeper_recovered_total` | type | 복구 통계 |

### OpenAI

| 메트릭명 | 태그 | 설명 |
|---------|------|------|
| `openai_api_requests_total` | model, schema, status | API 요청 통계 |
| `openai_api_latency_seconds` | model, schema, status | API 응답 시간 (p50/p90/p99) |

---

## 로그 이벤트 타입 (event_type)

| event_type | 설명 |
|-----------|------|
| `ai_job_start` | Job 처리 시작 |
| `ai_job_error` | Job 처리 중 에러 |
| `ai_job_suspended` | SUSPENDED 상태 전환 |
| `ai_job_retry_wait` | RETRY_WAIT 상태 전환 |
| `ai_job_failed` | FAILED 상태 전환 |
| `ai_worker_found` | Worker가 처리할 Job 발견 |
| `ai_worker_error` | Worker 실행 중 에러 |
| `ai_lock_failed` | Redis 락 획득 실패 |
| `ai_already_running` | Job이 이미 RUNNING 상태 |
| `ai_api_error` | OpenAI API 에러 |
| `ai_api_timeout` | OpenAI API Timeout |
| `ai_sweeper_start` | Sweeper 실행 시작 |
| `ai_sweeper_complete` | Sweeper 완료 |
| `ai_job_stuck_recovered` | Stuck Job 복구 |
| `ai_job_retry_wait_to_pending` | RETRY_WAIT → PENDING 전환 |

---

## TODO / 개선점

1. **Dead Letter Queue**: 최종 실패(FAILED) Job에 대한 알림/재처리 메커니즘
2. **배치 크기 동적 조정**: 부하에 따라 batchSize 자동 조절
3. **Circuit Breaker**: OpenAI API 장애 시 빠른 실패 처리
4. **Job 우선순위**: 긴급 기사에 대한 우선 처리 지원
5. **메트릭 대시보드**: Grafana 대시보드 템플릿 추가

---

## 분석 근거 파일 목록

### 크롤링

- `domain/naver/application/usecase/NaverEconomyCrawlScheduler.java`
- `domain/naver/domain/service/NaverCrawlerService.java`
- `domain/naver/persistence/entity/NaverArticleEntity.java`
- `domain/naver/domain/constant/NaverEconomySection.java`
- `global/config/NaverCrawlerProperties.java`

### AI Job

- `domain/ai/domain/service/AiJobService.java`
- `domain/ai/domain/worker/AiJobWorker.java`
- `domain/ai/domain/worker/AiJobSweeper.java`
- `domain/ai/domain/lock/AiJobLockService.java`
- `domain/ai/persistence/entity/AiJobEntity.java`
- `domain/ai/persistence/entity/AiJobType.java`
- `domain/ai/persistence/entity/AiJobStatus.java`
- `domain/ai/persistence/repository/AiJobRepository.java`
- `domain/ai/presentation/AiAdminController.java`

### OpenAI 연동

- `domain/ai/domain/client/OpenAiClient.java`
- `domain/ai/exception/code/AiErrorCode.java`

### 메트릭

- `domain/ai/domain/metrics/AiMetrics.java`

### 설정

- `resources/application.yaml`
