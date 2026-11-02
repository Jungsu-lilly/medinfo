package com.medinfo.nonpay.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 비급여 항목 이름 다듬기 + 검색어 동의어 */
final class ItemNames {

    /** 사람들이 검색하는 말 ↔ 심평원 항목명에 쓰이는 말 */
    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "mri", List.of("자기공명"),
            "자기공명", List.of("MRI"),
            "ct", List.of("전산화단층"),
            "독감", List.of("인플루엔자"),
            "인플루엔자", List.of("독감"),
            "엑스레이", List.of("X-ray", "단순촬영")
    );

    private ItemNames() {
    }

    /** "상급병실료/1인실" → "1인실". 슬래시 뒤가 이름이고 앞은 분류(경로로 따로 보여줌) */
    static String shortName(String korNm) {
        if (korNm == null) {
            return null;
        }
        int slash = korNm.indexOf('/');
        String tail = slash >= 0 ? korNm.substring(slash + 1).trim() : korNm.trim();
        return tail.isEmpty() ? korNm.trim() : tail;
    }

    /** 분류 경로: 중분류 › 소분류 › 상세분류 (같은 이름 반복·빈 값 제거) */
    static List<String> path(String mdiv, String sdiv, String dtl) {
        Set<String> parts = new LinkedHashSet<>();
        for (String p : new String[]{mdiv, sdiv, dtl}) {
            if (p != null && !p.isBlank()) {
                parts.add(p.trim());
            }
        }
        return new ArrayList<>(parts);
    }

    /**
     * "무릎 MRI" → [[무릎], [MRI, 자기공명]]. 단어끼리는 AND, 단어 안의 동의어끼리는 OR.
     * 너무 긴 검색어는 앞 5단어만.
     */
    static List<List<String>> terms(String query) {
        List<List<String>> out = new ArrayList<>();
        if (query == null) {
            return out;
        }
        for (String word : query.trim().split("\\s+")) {
            if (word.isBlank() || out.size() >= 5) {
                continue;
            }
            List<String> alts = new ArrayList<>();
            alts.add(word);
            alts.addAll(SYNONYMS.getOrDefault(word.toLowerCase(Locale.ROOT), List.of()));
            out.add(alts.stream().distinct().toList());
        }
        return out;
    }

    /** ILIKE 패턴: %, _ 같은 특수문자는 글자 그대로 찾게 이스케이프 */
    static String like(String s) {
        return "%" + s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
