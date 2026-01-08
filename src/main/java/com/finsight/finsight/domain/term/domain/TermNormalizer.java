package com.finsight.finsight.domain.term.domain;

import java.util.Locale;

public class TermNormalizer {

    public static String normalize(String raw) {
        if (raw == null) return null;

        String t = raw.trim().replaceAll("\\s+", " ");
        // 영문/숫자 위주면 소문자화 (한글은 영향 없음)
        if (t.matches(".*[A-Za-z].*")) {
            t = t.toLowerCase(Locale.ROOT);
        }
        return t;
    }

    private TermNormalizer() {}
}
