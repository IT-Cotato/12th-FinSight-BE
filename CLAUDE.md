# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build production JAR
./gradlew clean bootJar

# Full build with tests
./gradlew clean build

# Run tests
./gradlew test

# Run single test class
./gradlew test --tests "com.finsight.finsight.SomeTest"

# Run application locally
./gradlew bootRun
```

## Architecture Overview

FinSight는 금융 뉴스 수집 및 AI 분석 플랫폼으로, **도메인 주도 설계(DDD)** 기반의 레이어드 아키텍처를 사용한다.

### Package Structure

```
src/main/java/com/finsight/finsight/
├── domain/           # 도메인 모듈 (각 도메인별 독립적 구조)
│   ├── ai/           # AI 처리 (요약, 용어 추출, 인사이트, 퀴즈 생성)
│   ├── auth/         # 인증 (이메일/카카오 로그인, JWT)
│   ├── naver/        # 네이버 뉴스 크롤러
│   ├── term/         # 금융 용어 관리
│   └── user/         # 사용자 관리
└── global/           # 공통 모듈
    ├── config/       # 설정 (Security, Swagger, OpenAI 등)
    ├── exception/    # 전역 예외 처리
    ├── response/     # 표준 응답 DTO
    └── security/     # JWT, Spring Security
```

### Domain Module Structure

각 도메인 모듈은 다음 구조를 따른다:
```
domain/[module]/
├── presentation/     # REST Controller
├── application/      # DTO, Mapper, UseCase
├── domain/           # Service, Client, Business Logic
├── persistence/      # Entity, Repository
└── exception/        # 도메인별 예외
```

### Core Data Flow

1. **뉴스 크롤링**: `NaverCrawlerService` → Naver 경제 섹션 크롤링 → DB 저장 → AI Job 큐 등록
2. **AI 처리 파이프라인** (Job Queue 패턴):
   - `SUMMARY` → `TERM_CARDS`, `INSIGHT`, `QUIZ_CONTENT` 생성
   - `TERM_CARDS` → `QUIZ_TERM` 생성
3. **백그라운드 워커**: `AiJobWorker`가 스케줄러로 pending job 처리

### Key Integrations

- **OpenAI API**: gpt-4o-mini 모델 (JSON Schema 모드)
- **Kakao OAuth**: 소셜 로그인
- **Oracle DB**: Hibernate ORM
- **Prometheus/Grafana**: 메트릭 모니터링 (`/actuator/prometheus`)

## Git Conventions

- **브랜치**: `[type]/#[issue]-[description]` (예: `feat/#10-login`)
- **타입**: feat, fix, refactor, chore, docs, test
- **머지**: Squash & Merge
- **커밋 이모지**:
  - `:sparkles:` feat, `:bug:` fix, `:recycle:` refactor
  - `:white_check_mark:` test, `:memo:` docs

## Environment Variables

```
DB_URL, DB_USERNAME, DB_PASSWORD
JWT_SECRET
KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET, KAKAO_REDIRECT_URI
MAIL_USERNAME, MAIL_PASSWORD
OPENAI_API_KEY
```

## Feature Toggles

`application.yaml`에서 백그라운드 작업 활성화/비활성화:
```yaml
naver.crawler.enabled: true/false
ai.worker.enabled: true/false
```
