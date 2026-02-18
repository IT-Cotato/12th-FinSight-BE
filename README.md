# FinSight Backend

<div align="center">

**금융 뉴스 AI 분석 플랫폼**

금융 뉴스를 크롤링하여 AI 기반 요약, 용어 설명, 인사이트, 퀴즈를 자동 생성하는 백엔드 서비스

</div>
<p align="center">
  <img width="300" alt="1" src="https://github.com/user-attachments/assets/3c27af3d-5a5a-480e-9167-05bbf0375af0" />
  <img width="300" alt="2" src="https://github.com/user-attachments/assets/c06e3138-5010-4609-b8b2-89142c228bba" />
  <img width="300" alt="3" src="https://github.com/user-attachments/assets/68d3d12f-4d9f-49be-9812-699f69eeccfc" />
</p>


---

## 목차

1. [프로젝트 소개](#1-프로젝트-소개)
2. [기술 스택](#2-기술-스택)
3. [핵심 기능](#3-핵심-기능)
4. [시스템 아키텍처](#4-시스템-아키텍처)
5. [ERD](#5-erd)
6. [트러블슈팅](#6-트러블슈팅)
7. [팀원 소개](#7-팀원-소개)
8. [Git Conventions](#8-git-conventions)

---

## 1. 프로젝트 소개

**FinSight**는 금융 뉴스를 쉽게 이해하고 학습할 수 있도록 돕는 AI 기반 플랫폼입니다.

### 주요 특징

- **뉴스 자동 수집**: 네이버 경제 뉴스 8개 섹션 실시간 크롤링
- **AI 분석**: OpenAI GPT-4o-mini 기반 뉴스 요약 및 인사이트 생성
- **금융 용어 학습**: 뉴스에서 핵심 금융 용어 추출 및 설명 카드 제공
- **퀴즈 시스템**: AI 생성 퀴즈로 학습 내용 복습
- **푸시 알림**: 일일/주간 학습 리포트 FCM 알림

### 프로젝트 기간

- 2024.12 ~ 2025.02

---

## 2. 기술 스택

### Backend

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-0085CA?style=for-the-badge)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

### Database & Cache

![Oracle](https://img.shields.io/badge/Oracle-F80000?style=for-the-badge&logo=oracle&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### AI & External Services

![OpenAI](https://img.shields.io/badge/OpenAI_GPT--4o--mini-412991?style=for-the-badge&logo=openai&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase_FCM-DD2C00?style=for-the-badge&logo=firebase&logoColor=white)
![Kakao](https://img.shields.io/badge/Kakao_OAuth-FFCD00?style=for-the-badge&logo=kakao&logoColor=black)

### Monitoring & Infra

![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)
![Loki](https://img.shields.io/badge/Grafana_Loki-F46800?style=for-the-badge&logo=grafana&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Discord](https://img.shields.io/badge/Discord_Webhook-5865F2?style=for-the-badge&logo=discord&logoColor=white)

### Documentation

![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

---

## 3. 핵심 기능

### 3.1 CI/CD (GitHub Actions)

- **자동 배포**: PR merge 시 EC2 자동 배포
- **빌드 파이프라인**: JDK 17 + Gradle bootJar
- **모니터링 스택**: Docker Compose로 Prometheus/Promtail 관리

```
PR Merge → GitHub Actions → Build JAR → SCP to EC2 → Restart App
                                      → Deploy Monitoring Stack
```

### 3.2 카카오 소셜 로그인 & JWT 인증

- **카카오 OAuth 2.0**: Authorization Code Flow
- **JWT 토큰**: Access Token (1시간) / Refresh Token (30일)
- **Stateless 인증**: Spring Security + JWT Filter

### 3.3 이메일 인증

- **Gmail SMTP**: 인증 코드 발송
- **인증 코드**: 6자리 난수, 3분 유효
- **회원가입/비밀번호 재설정** 시 이메일 인증

### 3.4 FCM 푸시 알림

- **Firebase Cloud Messaging**: Android/iOS/Web 푸시
- **일일 알림**: 매일 오전 9시 학습 리마인더
- **주간 알림**: 매주 월요일 학습 리포트
- **기기별 토큰 관리**: 멀티 디바이스 지원

### 3.5 Grafana 모니터링 & Discord 알림

- **Grafana Cloud**: 메트릭/로그 통합 대시보드
- **Prometheus**: 애플리케이션 메트릭 수집 (JVM, HTTP, 커스텀 메트릭)
- **Loki**: 로그 수집 및 검색 (Promtail → Loki)
- **Discord Webhook**: Grafana Alert 연동으로 장애 알림 자동 전송
- **커스텀 대시보드**: 크롤링/AI Job/OpenAI API 실시간 모니터링

> 상세 문서: [docs/Metrics-Monitoring.md](docs/Metrics-Monitoring.md)

### 3.6 뉴스 크롤링

- **대상**: 네이버 경제 뉴스 8개 섹션
- **주기**: 5분마다 실행 (Cron 스케줄러)
- **중복 방지**: oid+aid 유니크 제약
- **JSoup**: HTML 파싱 및 DOM 조작

### 3.7 AI 비동기 작업 (Job Queue)

- **Job Types**: SUMMARY → TERM_CARDS → INSIGHT → QUIZ_CONTENT → QUIZ_TERM
- **상태 관리**: PENDING → RUNNING → SUCCESS/FAILED/RETRY_WAIT/SUSPENDED
- **분산락**: Redis 기반 중복 실행 방지
- **자동 복구**: Sweeper가 stuck Job 감지 및 재시도
- **지수 백오프**: 30초 × 2^retryCount

> 상세 문서: [docs/AI-Architecture-Crawling-to-AI-Jobs.md](docs/AI-Architecture-Crawling-to-AI-Jobs.md)

---

## 4. 시스템 아키텍처


### 4.1 전체 아키텍처

<img width="1259" height="929" alt="drawio_1" src="https://github.com/user-attachments/assets/3b7ca2b5-dfaa-46e8-af1f-cc9124898a5a" />


### 4.2 크롤링 → AI 비동기 작업 흐름

```
[Naver 경제 뉴스] ──► [NaverCrawlScheduler] ──► [NaverCrawlerService]
                            (5분 주기)              (JSoup 파싱)
                                                        │
                                                        ▼
                                               [NaverArticleEntity]
                                                   (DB 저장)
                                                        │
                                                        ▼ 본문 >= 1000자
                                               [AiJobService.enqueueSummary()]
                                                        │
┌───────────────────────────────────────────────────────┘
│
▼
[AiJobWorker] ──► [Redis 분산락] ──► [OpenAI API 호출]
  (30초 주기)                              │
                                           ▼
                              ┌────────────────────────────┐
                              │        Job 처리 순서        │
                              │                            │
                              │  SUMMARY ──► TERM_CARDS    │
                              │      │           │         │
                              │      ▼           ▼         │
                              │  INSIGHT    QUIZ_TERM      │
                              │      │                     │
                              │      ▼                     │
                              │  QUIZ_CONTENT              │
                              └────────────────────────────┘
                                           │
                                           ▼
                              [AiJobSweeper] (1분 주기)
                              - stuck Job 복구
                              - 재시도 스케줄링
```

### 4.3 모니터링 아키텍처

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
         └──────────┬──────────────────┘
                    ▼
         ┌─────────────────────┐
         │   Grafana Cloud     │
         │  ┌───────────────┐  │
         │  │  Prometheus   │  │
         │  │  (Metrics)    │  │
         │  └───────────────┘  │
         │  ┌───────────────┐  │         ┌─────────────┐
         │  │     Loki      │  │────────►│   Discord   │
         │  │   (Logs)      │  │  Alert  │   Webhook   │
         │  └───────────────┘  │         └─────────────┘
         │  ┌───────────────┐  │
         │  │  Dashboard    │  │
         │  └───────────────┘  │
         └─────────────────────┘
```

---

## 5. ERD

<p align="center">
  <img width="700" alt="final_finsight" src="https://github.com/user-attachments/assets/ab5275ef-a540-4d38-82cb-62b8ebc45ff1" />
</p>

---

## 6. 트러블슈팅

### 1. 서버 응답 지연 해결: 리전 이전 (Stockholm → Seoul)

- **증상**: API/Swagger 로딩이 전반적으로 느림
- **오해(1차)**: EC2 메모리 부족이라 판단 → 인스턴스 업그레이드했지만 효과 미미
- **원인(2차)**: 서버 리전이 **Stockholm(eu-north-1)** 이라 한국에서 RTT **200ms+** 레이턴시 발생
- **해결**: AMI로 서버 환경 그대로 복제 후 **서울(ap-northeast-2)** 로 이전
- **결과**: RTT **200 ~ 300ms → 20 ~ 40ms**, 체감 속도 **5 ~ 10배 개선**


### 2. AI 즉시 생성 지연/불안정 해결: DB Queue 비동기 처리 + 메트릭 모니터링

- **증상**: 뉴스 조회 시 AI(요약/용어/인사이트/퀴즈) 즉시 생성으로 **응답 지연·타임아웃** 발생
    + 백그라운드 처리 특성상 **실패(429/5xx/timeout)/Quota 부족**을 바로 파악하기 어려움
- **원인**: OpenAI 호출이 요청-응답 흐름에 포함되어 latency가 커지고, 장애 시 운영 관측이 어려움
- **해결**
    - 기사 저장 후 조건 충족 시 **AI Job을 DB Queue(PENDING)** 에 등록 → `AiJobWorker`가 비동기 처리  
      (상태 전이·재시도·Sweeper 복구·Redis 분산락으로 중복 방지)
    - **Micrometer 커스텀 메트릭**으로 Job 처리 결과/큐 크기/처리 시간 + OpenAI 성공률/에러코드/레이턴시 수집  
      → **Prometheus → Grafana Cloud** 대시보드 & **Discord 알림** 연동
- **결과**: 사용자 API는 **항상 빠르게 유지**, AI 오류 급증·Quota 부족·Job 적체(stuck)를 **대시보드/알림으로 즉시 감지** 가능


---

## 7. 팀원 소개

| 김남연 | 김민규 |                                           김세현                                            |
|:---:|:---:|:----------------------------------------------------------------------------------------:|
| <img src="https://github.com/ramen0519.png" width="150" height="150" /> | <img src="https://github.com/kingmingyu.png" width="150" height="150" /> |          <img src="https://github.com/kkshyun.png" width="150" height="150" />           |
| [@ramen0519](https://github.com/ramen0519) | [@kingmingyu](https://github.com/kingmingyu) |                          [@kkshyun](https://github.com/kkshyun)                          |
| **Auth** / **Notification**(Email, FCM)<br>Category API<br>Storage API<br>Quiz API | **Infra**(Deploy, CI/CD)<br>Learning API<br>MyPage API | **Crawling** / **AI Data Gen**<br>Monitoring(Metrics, Log)<br>Alert(Discord)<br>Home API |

---

## 8. Git Conventions

### 브랜치 전략

- **브랜치명**: `[type]/#[issue]-[description]`
- **예시**: `feat/#12-login`, `fix/#24-ai-job-retry`
- **머지 방식**: Squash & Merge

### 커밋 메시지

```
#<issue_number> :Emoji: <type>: <subject>
예: #1 :sparkles: feat: 로그인 기능 구현
```

| Type | Emoji | Description |
|------|-------|-------------|
| feat | :sparkles: | 새로운 기능 추가 |
| fix | :bug: | 버그 수정 |
| docs | :memo: | 문서 수정 |
| style | :art: | 코드 포맷팅 |
| refactor | :recycle: | 코드 리팩토링 |
| test | :white_check_mark: | 테스트 코드 |
| chore | :wrench: | 빌드/설정 수정 |

### Git Flow

```
1. 이슈 생성 (GitHub Issues)
2. develop 브랜치 최신화
   git checkout develop && git pull origin develop
3. 브랜치 생성
   git checkout -b feat/#12-login
4. 작업 및 커밋
5. PR 생성 (→ develop)
6. 리뷰 후 Squash & Merge
```
