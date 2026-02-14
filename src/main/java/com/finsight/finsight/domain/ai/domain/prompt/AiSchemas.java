package com.finsight.finsight.domain.ai.domain.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OpenAI JSON Schema (response_format=json_schema) 용 스키마 모음
 *
 * - SUMMARY: summary3 (string[3]) + summaryFull (string)
 * - TERM_CARDS: cards (object[3]) { term, highlightText, definition }
 * - INSIGHT: insights (object[3]) { title, detail, whyItMatters }
 * - QUIZ: questions (object[3]) { question, options[4], answerIndex(0~3), explanations[4] }
 */
public final class AiSchemas {

    private static final ObjectMapper OM = new ObjectMapper();

    private AiSchemas() {}

    // =========================================================
    // SUMMARY
    // { summary3: [ "…", "…", "…" ], summaryFull: "…" }
    // =========================================================
    public static JsonNode summarySchema() {
        ObjectNode root = OM.createObjectNode();
        root.put("type", "object");

        ObjectNode props = OM.createObjectNode();

        ObjectNode summary3 = OM.createObjectNode();
        summary3.put("type", "array");
        summary3.put("minItems", 3);
        summary3.put("maxItems", 3);
        summary3.set("items", stringSchema());
        props.set("summary3", summary3);

        props.set("summaryFull", stringSchema());

        root.set("properties", props);
        root.set("required", OM.createArrayNode().add("summary3").add("summaryFull"));
        root.put("additionalProperties", false);
        return root;
    }

    // =========================================================
    // TERM_CARDS
    // { cards: [ {term, highlightText, definition}, ... (3개) ] }
    // definition: "기사 한정 정의" 금지 (프롬프트에서 강제)
    // =========================================================
    public static JsonNode termCardsSchema() {
        ObjectNode root = OM.createObjectNode();
        root.put("type", "object");

        ObjectNode props = OM.createObjectNode();

        ObjectNode card = OM.createObjectNode();
        card.put("type", "object");
        ObjectNode cardProps = OM.createObjectNode();
        cardProps.set("term", stringSchema());
        cardProps.set("highlightText", stringSchema());
        cardProps.set("definition", stringSchema());
        card.set("properties", cardProps);
        card.set("required", OM.createArrayNode().add("term").add("highlightText").add("definition"));
        card.put("additionalProperties", false);

        ObjectNode cards = OM.createObjectNode();
        cards.put("type", "array");
        cards.put("minItems", 3);
        cards.put("maxItems", 3);
        cards.set("items", card);

        props.set("cards", cards);

        root.set("properties", props);
        root.set("required", OM.createArrayNode().add("cards"));
        root.put("additionalProperties", false);
        return root;
    }

    // =========================================================
    // INSIGHT
    // { insights: [ {title, detail, whyItMatters}, ... (3개) ] }
    // =========================================================
    public static JsonNode insightSchema() {
        ObjectNode root = OM.createObjectNode();
        root.put("type", "object");

        ObjectNode props = OM.createObjectNode();

        ObjectNode insight = OM.createObjectNode();
        insight.put("type", "object");
        ObjectNode insightProps = OM.createObjectNode();
        insightProps.set("title", stringSchema());
        insightProps.set("detail", stringSchema());
        insightProps.set("whyItMatters", stringSchema());
        insight.set("properties", insightProps);
        insight.set("required", OM.createArrayNode().add("title").add("detail").add("whyItMatters"));
        insight.put("additionalProperties", false);

        ObjectNode insights = OM.createObjectNode();
        insights.put("type", "array");
        insights.put("minItems", 3);
        insights.put("maxItems", 3);
        insights.set("items", insight);

        props.set("insights", insights);

        root.set("properties", props);
        root.set("required", OM.createArrayNode().add("insights"));
        root.put("additionalProperties", false);
        return root;
    }

    // =========================================================
    // QUIZ (content / term 공용)
    // {
    //   questions: [
    //     { question: "...", options: ["A","B","C","D"], answerIndex: 0~3, explanation: "..." },
    //     ... (3개)
    //   ]
    // }
    // =========================================================
    public static JsonNode quizSchema() {
        ObjectNode root = OM.createObjectNode();
        root.put("type", "object");

        ObjectNode props = OM.createObjectNode();

        ObjectNode question = OM.createObjectNode();
        question.put("type", "object");

        ObjectNode qProps = OM.createObjectNode();
        qProps.set("question", stringSchema());

        ObjectNode options = OM.createObjectNode();
        options.put("type", "array");
        options.put("minItems", 4);
        options.put("maxItems", 4);
        options.set("items", stringSchema());
        qProps.set("options", options);

        ObjectNode answerIndex = OM.createObjectNode();
        answerIndex.put("type", "integer");
        answerIndex.put("minimum", 0);
        answerIndex.put("maximum", 3);
        qProps.set("answerIndex", answerIndex);

        ObjectNode explanations = OM.createObjectNode();
        explanations.put("type", "array");
        explanations.put("minItems", 4);
        explanations.put("maxItems", 4);
        explanations.set("items", stringSchema());
        qProps.set("explanations", explanations);

        question.set("properties", qProps);
        question.set("required", OM.createArrayNode()
                .add("question")
                .add("options")
                .add("answerIndex")
                .add("explanations"));
        question.put("additionalProperties", false);

        ObjectNode questions = OM.createObjectNode();
        questions.put("type", "array");
        questions.put("minItems", 3);
        questions.put("maxItems", 3);
        questions.set("items", question);

        props.set("questions", questions);

        root.set("properties", props);
        root.set("required", OM.createArrayNode().add("questions"));
        root.put("additionalProperties", false);
        return root;
    }

    // =========================================================
    // small helpers
    // =========================================================
    private static ObjectNode stringSchema() {
        ObjectNode s = OM.createObjectNode();
        s.put("type", "string");
        return s;
    }
}
