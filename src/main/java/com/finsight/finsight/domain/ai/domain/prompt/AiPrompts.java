package com.finsight.finsight.domain.ai.domain.prompt;

import java.time.LocalDateTime;

public class AiPrompts {

    public static final String COMMON_SYSTEM = """
        당신은 경제 뉴스 편집자이자 분석가입니다.
        출력은 반드시 한국어로 작성합니다.
        추측/단정은 금지하고, 주어진 텍스트(요약문)에 근거해서만 작성합니다.
        반드시 지정된 JSON 스키마에 맞춰 응답합니다.
        """;

    public static String summaryUser(String title, String press, LocalDateTime publishedAt, String content) {
        return """
            [기사 정보]
            - 제목: %s
            - 언론사: %s
            - 발행시각: %s

            [기사 본문]
            %s

            [요구사항]
            1) summary3: 3줄 요약 (각 줄은 40~80자 정도, 핵심 사실 위주)
            2) summaryFull: 전체 본문 요약 (약 700~1200자, 맥락/원인-결과 포함, 마크업/하이라이트 금지)
            """.formatted(safe(title), safe(press), String.valueOf(publishedAt), safe(content));
    }

    public static String termCardsUser(String summaryFull) {
        return """
        [전체 요약문]
        %s

        [요구사항]
        - 핵심 용어/개념 3개를 선택해 cards로 반환합니다.
        - term: 용어(짧게)
        - highlightText: 위 '전체 요약문'에서 해당 term이 등장하는 문장/구절 1개를 그대로 인용해 주세요.
          (중요: highlightText는 요약문에 실제로 존재하는 문장/구절이어야 합니다.)
        - definition: '일반적으로 통용되는 의미'로 1~2문장 설명(교과서/사전식 톤)
          * 기사에만 맞춘 정의(특정 기업/사건/수치/이번 기사에서만 성립하는 설명)는 금지합니다.
          * 예: "이번 기사에서 A기업이..." / "이번 발표에서..." 같은 기사-종속 설명 금지
          * 대신: 개념 자체의 의미/역할/경제적 맥락을 설명
          * 경제/금융 문맥을 우선합니다. (동일 단어가 여러 의미면, 경제/금융에서 통용되는 의미로 선택)
        - 3개 용어는 서로 겹치지 않게, 요약문 내용을 대표하는 것 위주로 뽑아 주세요.
        """.formatted(safe(summaryFull));
    }



    public static String insightUser(String summaryFull) {
        return """
            [전체 요약문]
            %s

            [요구사항]
            - oneLineTakeaway: 한 줄로 핵심 결론(1문장)
            - keyInsights: 인사이트 3개(각 1문장)
            - watchPoints: 앞으로 체크할 포인트 3개(각 1문장)
            - possibleImpacts: 시장/산업/가계에 미칠 가능 영향 2개(각 1문장)
            """.formatted(safe(summaryFull));
    }

    public static String quizContentUser(String summaryFull) {
        return """
            [전체 요약문]
            %s

            [요구사항]
            - 4지선다 퀴즈 3문항 생성
            - question: 질문(요약문 근거)
            - choices: 보기 4개
            - answerIndex: 정답 보기 인덱스(0~3)
            - explanation: 왜 이게 정답인지 1~2문장
            - 보기들은 그럴듯하게 만들되, 요약문 근거로 명확히 판별 가능해야 함
            """.formatted(safe(summaryFull));
    }

    public static String quizTermUser(String termCardsText) {
        return """
            [용어 카드]
            %s

            [요구사항]
            - 위 용어 카드 기반으로 4지선다 퀴즈 3문항 생성
            - question: 용어 의미/맥락을 묻는 질문
            - choices: 보기 4개 (1개만 정답)
            - answerIndex(0~3), explanation 포함
            """.formatted(safe(termCardsText));
    }


    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}
