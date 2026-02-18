# Metrics & Monitoring

FinSight 백엔드의 메트릭 수집 및 모니터링 시스템 문서입니다.
<br/>

<p align="center">
  <img width="600" alt="image1" src="https://github.com/user-attachments/assets/0d18396e-01d4-41bb-b435-9642d64d2e31" />
  <img width="300" alt="IMG_0509" src="https://github.com/user-attachments/assets/131b28c3-d7f3-410a-a8ca-17c9d804eee3" />
</p>


---

## 목차

1. [아키텍처 개요](#아키텍처-개요)
2. [메트릭 수집](#메트릭-수집)
3. [로그 수집](#로그-수집)
4. [Grafana 대시보드](#grafana-대시보드)
5. [Discord 알림](#discord-알림)
6. [인프라 설정](#인프라-설정)

---

## 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                      │
│  ┌─────────────┐              ┌─────────────┐                   │
│  │ Micrometer  │              │   SLF4J     │                   │
│  │  Metrics    │              │  Logback    │                   │
│  └──────┬──────┘              └──────┬──────┘                   │
└─────────┼────────────────────────────┼──────────────────────────┘
          │                            │
          ▼                            ▼
   /actuator/prometheus         /var/log/finsight/
          │                            │
          ▼                            ▼
┌─────────────────┐           ┌─────────────────┐
│   Prometheus    │           │    Promtail     │
│   (Docker)      │           │   (Docker)      │
└────────┬────────┘           └────────┬────────┘
         │                             │
         │    Remote Write             │    Push
         ▼                             ▼
┌─────────────────────────────────────────────────┐
│                  Grafana Cloud                  │
│  ┌───────────────┐    ┌───────────────┐        │
│  │  Prometheus   │    │     Loki      │        │
│  │  (Metrics)    │    │   (Logs)      │        │
│  └───────────────┘    └───────────────┘        │
│                                                 │
│  ┌───────────────┐    ┌───────────────┐        │
│  │   Dashboard   │    │    Alerting   │───────────► Discord
│  └───────────────┘    └───────────────┘        │
└─────────────────────────────────────────────────┘
```

### 구성 요소

| 컴포넌트 | 역할 | 위치 |
|----------|------|------|
| **Micrometer** | 애플리케이션 메트릭 수집 | Spring Boot 내장 |
| **Prometheus** | 메트릭 스크래핑 및 전송 | Docker (EC2) |
| **Promtail** | 로그 수집 및 전송 | Docker (EC2) |
| **Grafana Cloud** | 메트릭/로그 저장 및 시각화 | SaaS |
| **Discord Webhook** | 알림 전송 | Grafana Alerting |

---

## 메트릭 수집

### Spring Boot Actuator

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

**엔드포인트**: `GET /actuator/prometheus`

### 기본 메트릭 (Micrometer)

| 메트릭 | 설명 |
|--------|------|
| `jvm_memory_used_bytes` | JVM 힙 메모리 사용량 |
| `jvm_threads_live_threads` | 활성 스레드 수 |
| `system_cpu_usage` | CPU 사용률 |
| `http_server_requests_seconds` | HTTP 요청 처리 시간 |

### 커스텀 메트릭

#### AI Job 메트릭 (`AiMetrics.java`)

| 메트릭 | 타입 | 태그 | 설명 |
|--------|------|------|------|
| `ai_jobs_enqueued_total` | Counter | type, result | Job 등록 통계 |
| `ai_jobs_processed_total` | Counter | type, result, error_code | Job 처리 결과 |
| `ai_jobs_events_total` | Counter | type, event | Job 이벤트 |
| `ai_job_duration_seconds` | Timer | type | Job 처리 시간 |
| `ai_jobs_queue_size` | Gauge | type, status | 상태별 큐 크기 |
| `ai_articles_completed_total` | Counter | - | AI 완료된 기사 수 |
| `ai_sweeper_events_total` | Counter | event | Sweeper 이벤트 |
| `ai_sweeper_recovered_total` | Counter | type | 복구 통계 |

#### 크롤러 메트릭

| 메트릭 | 타입 | 태그 | 설명 |
|--------|------|------|------|
| `crawler_articles_total` | Counter | section, status | 섹션별/상태별 기사 수 |
| `crawler_run_seconds` | Timer | section | 크롤링 소요 시간 |
| `crawler_fetch_seconds` | Timer | type | HTTP 요청 시간 |

#### OpenAI API 메트릭

| 메트릭 | 타입 | 태그 | 설명 |
|--------|------|------|------|
| `openai_api_requests_total` | Counter | model, schema, status | API 요청 통계 |
| `openai_api_latency_seconds` | Timer | model, schema, status | API 응답 시간 |

### 메트릭 사용 예시

```java
@Component
@RequiredArgsConstructor
public class AiMetrics {
    private final MeterRegistry meterRegistry;

    public void incProcessed(AiJobType type, String result) {
        Counter.builder("ai_jobs_processed_total")
                .tag("type", type.name())
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, AiJobType type) {
        sample.stop(Timer.builder("ai_job_duration_seconds")
                .tag("type", type.name())
                .register(meterRegistry));
    }
}
```

---

## 로그 수집

### 애플리케이션 로깅

```yaml
# application.yaml
logging:
  file:
    name: /var/log/finsight/finsight.log
  logback:
    rollingpolicy:
      max-history: 7  # 7일간 보관
```

### Promtail 설정

```yaml
# promtail.yml
server:
  http_listen_port: 9080

clients:
  - url: https://logs-prod-030.grafana.net/loki/api/v1/push
    basic_auth:
      username: "${LOKI_USER}"
      password: "${LOKI_API_KEY}"

scrape_configs:
  - job_name: finsight
    static_configs:
      - targets: [localhost]
        labels:
          job: finsight
          environment: production
          __path__: /logs/*.log

    pipeline_stages:
      - regex:
          expression: '^(?P<time>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}).*?(?P<level>[A-Z]{4,5})\s+.*'
      - labels:
          level:
      - timestamp:
          source: time
          format: RFC3339
```

### 로그 레벨별 라벨

| 라벨 | 값 |
|------|-----|
| `job` | finsight |
| `environment` | local / production |
| `level` | DEBUG / INFO / WARN / ERROR |

---

## Grafana 대시보드

### 대시보드 패널 구성

**파일**: `infra/observability/grafana/dashboards/finsight-overview.json`

#### 1. System Overview

| 패널 | 쿼리 | 설명 |
|------|------|------|
| App Status | `up{service="finsight"}` | 애플리케이션 상태 (UP/DOWN) |
| Heap Memory Used | `jvm_memory_used_bytes{area="heap"}` | JVM 힙 메모리 |
| Live Threads | `jvm_threads_live_threads` | 활성 스레드 수 |
| HTTP Requests/sec | `rate(http_server_requests_seconds_count[5m])` | 초당 HTTP 요청 |
| CPU Usage | `system_cpu_usage * 100` | CPU 사용률 |

#### 2. AI Jobs

| 패널 | 쿼리 | 설명 |
|------|------|------|
| Pending Jobs | `ai_jobs_queue_size{status="PENDING"}` | 대기 중인 Job |
| AI Completed (1h) | `increase(ai_articles_completed_total[1h])` | 완료된 기사 수 |
| AI Jobs Processed | `increase(ai_jobs_processed_total[5m])` | Job 처리 결과 |
| AI Job Duration | `rate(ai_job_duration_seconds_sum[5m]) / rate(ai_job_duration_seconds_count[5m])` | 평균 처리 시간 |
| Sweeper Recovered | `increase(ai_sweeper_recovered_total[1h])` | 복구된 Job 수 |

#### 3. OpenAI API

| 패널 | 쿼리 | 설명 |
|------|------|------|
| API Success Rate | `sum(rate(openai_api_requests_total{status="success"}[1h])) / sum(rate(openai_api_requests_total[1h]))` | API 성공률 |
| OpenAI API Requests | `increase(openai_api_requests_total[5m])` | API 요청 추이 |
| API Errors by Code | `increase(openai_api_requests_total{status="error"}[1h])` | 에러 코드별 통계 |

#### 4. Crawler

| 패널 | 쿼리 | 설명 |
|------|------|------|
| Articles Saved (1h) | `increase(crawler_articles_total{status="saved"}[1h])` | 저장된 기사 수 |
| AI Skipped (Short) | `increase(crawler_articles_total{status="ai_skipped_short_content"}[1h])` | AI 스킵 기사 |
| Crawler Duration | `rate(crawler_run_seconds_sum[5m]) / rate(crawler_run_seconds_count[5m])` | 평균 크롤링 시간 |

#### 5. Logs (Loki)

| 패널 | 쿼리 | 설명 |
|------|------|------|
| Error Logs (1h) | `count_over_time({job="finsight"} \|= "[ERROR]" [1h])` | 에러 로그 수 |
| Login Failed (1h) | `count_over_time({job="finsight"} \|= "event_type=login_failed" [1h])` | 로그인 실패 |
| Login Success (1h) | `count_over_time({job="finsight"} \|= "event_type=login_success" [1h])` | 로그인 성공 |
| Signups (1h) | `count_over_time({job="finsight"} \|= "signup_success" [1h])` | 회원가입 |
| Recent Logs | `{job="finsight"} \|~ "(?i)ERROR\|WARN"` | 최근 에러/경고 로그 |

---

## Discord 알림

### Grafana Alert → Discord Webhook

Grafana Cloud의 Alerting 기능으로 특정 조건 충족 시 Discord로 알림을 전송합니다.

### 알림 조건 예시

| 알림 | 조건                            | 심각도 |
|------|-------------------------------|--------|
| App Down | `up{service="finsight"} == 0` | Critical |
| High Error Rate | `에러 로그 > 50개/1h`              | Warning |
| AI Job Stuck | `RUNNING 상태 Job > 10개`        | Warning |
| OpenAI API Errors | `API 에러율 > 5%`                | Warning |
| High Memory Usage | `힙 메모리 > 85%`                 | Warning |

### Discord 메시지 형식 예시

```
**Firing**

Value: A=0, C=1
Labels:
 - alertname = No AI Completion
 - grafana_folder = FinSight
Annotations:
 - summary = 1시간동안 AI 작업이 완성된 기사가 5건 미만입니다
Source: [Grafana Dashboard Link]
Silence: [Alert Silence Link]

```

---

## 인프라 설정

### Docker Compose

```yaml
# docker-compose.yml
services:
  prometheus-config:
    image: alpine:latest
    environment:
      - ENVIRONMENT=local
      - GRAFANA_REMOTE_WRITE_URL=${GRAFANA_REMOTE_WRITE_URL}
      - GRAFANA_CLOUD_USER=${GRAFANA_CLOUD_USER}
      - GRAFANA_CLOUD_API_KEY=${GRAFANA_CLOUD_API_KEY}
    volumes:
      - ./prometheus:/config
      - prometheus_config:/output
    command: >
      sh -c "apk add --no-cache gettext &&
             envsubst < /config/prometheus.yml.template > /output/prometheus.yml"

  prometheus:
    image: prom/prometheus:latest
    container_name: finsight-prometheus
    volumes:
      - prometheus_config:/etc/prometheus:ro
      - prometheus_data:/prometheus
    depends_on:
      prometheus-config:
        condition: service_completed_successfully

  promtail:
    image: grafana/promtail:latest
    container_name: finsight-promtail
    environment:
      - LOKI_URL=${LOKI_URL}
      - LOKI_USER=${LOKI_USER}
      - LOKI_API_KEY=${LOKI_API_KEY}
    volumes:
      - /var/log/finsight:/logs:ro
      - ./promtail/promtail.yml:/etc/promtail/config.yml:ro
    command: -config.file=/etc/promtail/config.yml -config.expand-env=true
```

### Prometheus 설정

```yaml
# prometheus.yml.template
global:
  scrape_interval: 60s
  external_labels:
    environment: "${ENVIRONMENT}"
    service: "finsight"

remote_write:
  - url: ${GRAFANA_REMOTE_WRITE_URL}
    basic_auth:
      username: ${GRAFANA_CLOUD_USER}
      password: ${GRAFANA_CLOUD_API_KEY}

scrape_configs:
  - job_name: "finsight-app"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["host.docker.internal:8080"]
```

### 환경 변수

| 변수 | 설명 |
|------|------|
| `GRAFANA_REMOTE_WRITE_URL` | Grafana Cloud Prometheus URL |
| `GRAFANA_CLOUD_USER` | Grafana Cloud 사용자 ID |
| `GRAFANA_CLOUD_API_KEY` | Grafana Cloud API 키 |
| `LOKI_URL` | Grafana Loki URL |
| `LOKI_USER` | Loki 사용자 ID |
| `LOKI_API_KEY` | Loki API 키 |

---

## 관련 파일

### 애플리케이션

- `domain/ai/domain/metrics/AiMetrics.java` - AI Job 메트릭
- `domain/naver/domain/service/NaverCrawlerService.java` - 크롤러 메트릭
- `domain/ai/domain/client/OpenAiClient.java` - OpenAI 메트릭

### 인프라

- `infra/observability/docker-compose.yml` - Docker Compose
- `infra/observability/prometheus/prometheus.yml.template` - Prometheus 설정
- `infra/observability/promtail/promtail.yml` - Promtail 설정
- `infra/observability/grafana/dashboards/finsight-overview.json` - 대시보드
