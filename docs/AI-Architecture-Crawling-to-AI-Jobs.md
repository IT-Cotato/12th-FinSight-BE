# AI Architecture - Crawling to AI Jobs

FinSight 백엔드의 **뉴스 크롤링 → AI 처리 파이프라인** 아키텍처 문서입니다.

---

## 목차

1. [전체 개요](#전체-개요)
2. [컴포넌트 맵](#컴포넌트-맵)
3. [크롤링 파이프라인](#크롤링-파이프라인)
4. [AI Job 파이프라인](#ai-job-파이프라인)
5. [Job 상태 관리](#job-상태-관리)
6. [장애 복구](#장애-복구)
7. [설정](#설정)

---

## 전체 개요

### 핵심 흐름

```
[Naver 경제 뉴스]
        │
        ▼ (5분 주기)
[NaverCrawlScheduler]
        │
        ▼
[NaverCrawlerService] ─── JSoup으로 HTML 파싱
        │
        ▼
[NaverArticleEntity] ─── DB 저장 (oid+aid Unique)
        │
        ▼ 본문 >= 1000자
[AiJobService.enqueueSummary()] ─── SUMMARY Job 생성
        │
        ▼ (30초 주기)
[AiJobWorker]
        │
        ▼
[Redis 분산락] ─── 중복 실행 방지
        │
        ▼
[OpenAI API 호출] ─── gpt-4o-mini (JSON Schema)
        │
        ▼
[후속 Job 생성] ─── TERM_CARDS → INSIGHT → QUIZ
        │
        ▼ (1분 주기)
[AiJobSweeper] ─── stuck Job 복구
```

---

## 컴포넌트 맵

### 크롤링 관련

| 클래스 | 역할 |
|--------|------|
| `NaverEconomyCrawlScheduler` | Cron 기반 스케줄러 (5분 주기) |
| `NaverCrawlerService` | 크롤링 로직 (섹션별 트랜잭션 분리) |
| `NaverArticleEntity` | 기사 엔티티 (oid, aid 유니크) |
| `NaverEconomySection` | 8개 경제 섹션 enum |

### AI Job 관련

| 클래스 | 역할 |
|--------|------|
| `AiJobService` | Job 생명주기 관리 (enqueue, process, 상태 전이) |
| `AiJobWorker` | PENDING Job 처리 (30초 주기) |
| `AiJobSweeper` | Stuck/RetryWait Job 복구 (1분 주기) |
| `AiJobLockService` | Redis 분산락 (중복 실행 방지) |
| `AiJobEntity` | Job 엔티티 (상태, 재시도, 에러 정보) |
| `AiJobType` | Job 유형 enum |
| `AiJobStatus` | Job 상태 enum |

### OpenAI 연동

| 클래스 | 역할 |
|--------|------|
| `OpenAiClient` | OpenAI API 호출 (JSON Schema 모드) |
| `AiErrorCode` | 에러 코드 정의 (suspendable, retryable 분류) |

### 메트릭

| 클래스 | 역할 |
|--------|------|
| `AiMetrics` | Micrometer 기반 메트릭 수집 |

---

## 크롤링 파이프라인

### 대상 섹션

| 섹션 | 설명 |
|------|------|
| FINANCE | 금융 |
| STOCK | 증권 |
| INDUSTRY | 산업/재계 |
| VENTURE | 중기/벤처 |
| REALESTATE | 부동산 |
| WORLD | 세계경제 |
| LIVING | 생활경제 |
| GENERAL | 경제일반 |

### 크롤링 프로세스

```java
// NaverEconomyCrawlScheduler.java
@Scheduled(cron = "${naver.crawler.cron}", zone = "Asia/Seoul")
public void run() {
    naverCrawlerService.crawlAllOnce();
}
```

1. **목록 페이지 수집** (JSoup)
   - `.section_latest` 영역에서만 링크 추출
   - 중복 제거 (LinkedHashSet)

2. **기사 상세 크롤링**
   - 제목, 본문, 발행일, 썸네일 추출
   - 발행일 파싱: meta tag → `data-date-time` → JSON-LD

3. **DB 저장**
   - Unique Constraint: (oid, aid)
   - `DataIntegrityViolationException` 처리로 레이스 컨디션 방지

4. **AI Job 등록**
   - 본문 >= `minContentLengthForAi` (기본 1000자)
   - `AiJobService.enqueueSummary(article)` 호출

---

## AI Job 파이프라인

### Job 처리 순서

```
SUMMARY
    │
    ├──► TERM_CARDS ──► QUIZ_TERM
    │
    ├──► INSIGHT
    │
    └──► QUIZ_CONTENT
```

각 Job은 이전 단계의 성공에 의존합니다.

### Worker 동작

```java
// AiJobWorker.java
@Scheduled(cron = "${ai.worker.cron:*/30 * * * * *}")
public void runScheduled() {
    for (AiJobType type : AiJobType.values()) {
        List<Long> jobIds = findPendingJobIds(type, batchSize);
        for (Long jobId : jobIds) {
            processJob(jobId);
        }
    }
}
```

1. PENDING Job 조회 (batchSize만큼)
2. Redis 락 획득 시도
3. PENDING → RUNNING 상태 전환
4. OpenAI API 호출
5. 성공/실패 처리
6. 락 해제

### OpenAI 연동

| 설정 | 값 |
|------|-----|
| Model | gpt-4o-mini |
| Timeout | 20초 |
| Max Output Tokens | 3000 |
| Temperature | 0.2 |
| Response Format | JSON Schema |

---

## Job 상태 관리

### 상태 전이도

```
(생성)
   │
   ▼
PENDING ◄─────────────────────────────────┐
   │                                      │
   ▼ tryMarkRunning()                     │
RUNNING ──────────────────────────────────┤
   │                                      │
   ├─► 성공 ───────────► SUCCESS          │
   │                                      │
   ├─► suspendable 에러 ► SUSPENDED       │
   │   (쿼터 소진, 인증 실패)              │
   │                                      │
   ├─► retryable 에러 ──► RETRY_WAIT ─────┘
   │   (429, 5xx, timeout)     (nextRunAt 도래 시)
   │
   └─► 재시도 초과 ─────► FAILED
```

### 상태 설명

| 상태 | 설명 |
|------|------|
| `PENDING` | 처리 대기 중 |
| `RUNNING` | Worker가 처리 중 |
| `SUCCESS` | 처리 완료 |
| `FAILED` | 최종 실패 (재시도 불가) |
| `RETRY_WAIT` | 재시도 대기 (nextRunAt 이후 PENDING으로) |
| `SUSPENDED` | 수동 확인 필요 (쿼터 소진, 인증 실패) |

### 재시도 정책

**지수 백오프**:
```
1차 재시도: 30초 후
2차 재시도: 60초 후 (30 × 2¹)
3차 재시도: 120초 후 (30 × 2²)
4차 재시도: 240초 후 (30 × 2³)
5차 재시도: 480초 후 (30 × 2⁴)
```

최대 재시도: 5회 (기본값)

---

## 장애 복구

### Sweeper 동작

```java
// AiJobSweeper.java
@Scheduled(cron = "${ai.sweeper.cron:0 */1 * * * *}")
public void sweep() {
    recoverStuckJobs();      // RUNNING 10분 초과 → RETRY_WAIT
    recoverRetryWaitJobs();  // RETRY_WAIT + nextRunAt 도래 → PENDING
}
```

### 장애 시나리오별 처리

| 시나리오 | 현상 | 처리 |
|----------|------|------|
| 워커 중단 | RUNNING 상태로 방치 | Sweeper가 10분 후 RETRY_WAIT로 전환 |
| OpenAI Quota 소진 | 402/insufficient_quota | SUSPENDED → 결제 후 Admin API로 재개 |
| Rate Limit (429) | 요청 과다 | RETRY_WAIT → 지수 백오프 재시도 |
| Timeout | API 응답 20초 초과 | RETRY_WAIT → 재시도 |
| 잘못된 요청 (400) | 프롬프트/스키마 오류 | FAILED (재시도 불가) |

### Admin API

```bash
# 단건 재개
POST /api/v1/admin/ai/jobs/{jobId}/resume

# 일괄 재개 (SUSPENDED 상태)
POST /api/v1/admin/ai/jobs/resume?reason=QUOTA
POST /api/v1/admin/ai/jobs/resume?reason=AUTH
```

---

## 설정

### application.yaml

```yaml
naver:
  crawler:
    enabled: false                    # 크롤링 활성화
    cron: "0 */5 * * * *"            # 5분마다
    max-pages: 1                      # 섹션당 최신 1페이지
    stop-after-seen-streak: 8         # 이미 저장된 기사 8개 연속 → 조기 종료
    timeout-ms: 8000
    sleep-min-ms: 250
    sleep-max-ms: 700
    min-content-length-for-ai: 1000   # 1000자 미만 AI 스킵

ai:
  worker:
    enabled: false                    # AI Worker 활성화
    cron: "*/30 * * * * *"           # 30초마다
    batch-size: 5                     # 한 번에 5개 Job 처리
  retry:
    max-attempts: 5                   # 최대 5회 재시도
    base-delay-seconds: 30            # 기본 지연 30초
    max-delay-seconds: 600            # 최대 지연 600초
  sweeper:
    enabled: false                    # Sweeper 활성화
    cron: "0 */1 * * * *"            # 1분마다
    stuck-threshold-minutes: 10       # RUNNING 10분 초과 시 stuck

openai:
  api-key: ${OPENAI_API_KEY}
  model: gpt-4o-mini
  timeout-ms: 20000
  max-output-tokens: 3000
  temperature: 0.2
```

---

## 관련 파일

### 크롤링

- `domain/naver/application/usecase/NaverEconomyCrawlScheduler.java`
- `domain/naver/domain/service/NaverCrawlerService.java`
- `domain/naver/persistence/entity/NaverArticleEntity.java`
- `global/config/NaverCrawlerProperties.java`

### AI Job

- `domain/ai/domain/service/AiJobService.java`
- `domain/ai/domain/worker/AiJobWorker.java`
- `domain/ai/domain/worker/AiJobSweeper.java`
- `domain/ai/domain/lock/AiJobLockService.java`
- `domain/ai/persistence/entity/AiJobEntity.java`
- `domain/ai/persistence/entity/AiJobType.java`
- `domain/ai/persistence/entity/AiJobStatus.java`

### OpenAI

- `domain/ai/domain/client/OpenAiClient.java`
- `domain/ai/exception/code/AiErrorCode.java`

### 메트릭

- `domain/ai/domain/metrics/AiMetrics.java`
