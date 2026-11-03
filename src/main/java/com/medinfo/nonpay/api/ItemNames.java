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
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("mri", List.of("자기공명")),
            Map.entry("엠알아이", List.of("MRI", "자기공명")),
            Map.entry("자기공명", List.of("MRI")),
            Map.entry("ct", List.of("전산화단층")),
            Map.entry("씨티", List.of("CT", "전산화단층")),
            Map.entry("독감", List.of("인플루엔자")),
            Map.entry("인플루엔자", List.of("독감")),
            Map.entry("엑스레이", List.of("X-ray", "단순촬영")),
            // 화면 이름("HPV(자궁경부암)", "MMR(홍역·볼거리·풍진)" …)엔 있지만 심평원 항목명엔 없는 말
            Map.entry("자궁경부암", List.of("사람유두종바이러스", "자궁경부")),
            Map.entry("볼거리", List.of("유행성이하선염")),
            Map.entry("뇌수막염", List.of("수막구균")),
            Map.entry("수막염", List.of("수막구균")),
            Map.entry("백신", List.of("예방접종")),
            Map.entry("예방주사", List.of("예방접종"))
    );

    /** 붙여 쓴 검색어 끝에서 떼어 내는 말: "자궁경부암백신" → "자궁경부암 백신" */
    private static final List<String> TRAILING_WORDS = List.of("예방접종", "예방주사", "백신");

    /**
     * 동의어 때문에 딸려 오는 다른 항목 빼기.
     * 독감 → 인플루엔자로 찾으면 b형헤모필루스인플루엔자(Hib, 영유아 뇌수막염 백신)도 걸린다
     */
    private static final Map<String, List<String>> EXCLUDES = Map.of(
            "독감", List.of("헤모필루스")
    );

    private ItemNames() {
    }

    /** 의학 용어 부위 → 사람들이 검색하는 말 (괄호로 원래 용어도 남김) */
    private static final Map<String, String> BODY_PARTS = Map.ofEntries(
            Map.entry("슬관절", "무릎"), Map.entry("견관절", "어깨"), Map.entry("요천추", "허리"),
            Map.entry("경추", "목"), Map.entry("흉추", "등"), Map.entry("주관절", "팔꿈치"),
            Map.entry("수관절", "손목"), Map.entry("손목관절", "손목"), Map.entry("발목관절", "발목"),
            Map.entry("고관절", "엉덩이"), Map.entry("뇌", "뇌"));

    /** 괄호 병기 없이 통째로 바꾸는 부위 (원래 말이 일반인에게 뜻이 안 통하는 경우). 키는 공백 없이 */
    private static final Map<String, String> RENAMES = Map.of(
            "관절외하지", "다리(관절 외)",
            "관절외상지", "팔(관절 외)");

    /** 초음파 항목명 끝에 붙는 등급·범위 (부위가 아님) */
    private static final Set<String> US_QUALIFIERS = Set.of("단순", "전문", "정밀");

    /** 길거나 낯선 질병명 → 사람들이 검색하는 말 */
    private static final Map<String, String> VACCINE_NAMES = Map.of(
            "사람유두종바이러스 감염증(HPV 백신)", "HPV(자궁경부암)");

    /**
     * 심평원 항목명(분류/세부/…) → 화면 이름. 실제 이름 예:
     *   자기공명영상진단료(MRI-기본검사)/근골격계-슬관절/일반                → 무릎(슬관절) MRI
     *   자기공명영상진단료(MRI-기본검사)/근골격계-견관절/조영제 주입 전·후 촬영 판독 → 어깨(견관절) MRI (조영제)
     *   자기공명영상진단료(MRI-기본검사)/근골격계-관절외 하지/일반           → 다리(관절 외) MRI
     *   초음파검사료(진단초음파)/두경부-경부 초음파/갑상선·부갑상선          → 갑상선·부갑상선 초음파
     *   초음파검사료(진단초음파)/근골격, 연부-연부조직 초음파/일반           → 연부조직 초음파
     *   초음파검사료(기본초음파)/단순초음파/Ⅱ                              → 단순초음파 Ⅱ
     *   초음파검사료(진단초음파)/복부-비뇨기계 초음파/방광                   → 방광 초음파
 *   초음파검사료(진단초음파)/임산부 초음파/제1삼분기 -일반               → 임산부 초음파 (제1삼분기)
 *   예방접종료/대상포진/싱그릭스주                                     → 대상포진 예방접종 (싱그릭스주)
     *   상급병실료/1인실                                                  → 상급병실료 1인실
     *   제증명수수료/진단서/일반                                           → 진단서
     * 규칙에 안 맞으면 분류 뒤를 " · "로 이어 붙인다.
     */
    static String shortName(String korNm) {
        if (korNm == null) {
            return null;
        }
        List<String> segs = new ArrayList<>();
        for (String p : korNm.split("/")) {
            if (!p.isBlank()) {
                segs.add(p.trim());
            }
        }
        if (segs.size() <= 1) {
            return korNm.trim();
        }
        String category = segs.get(0);
        List<String> rest = segs.subList(1, segs.size());

        if (category.contains("자기공명") || category.contains("MRI")) {
            return mri(category, rest);
        }
        if (category.contains("초음파")) {
            return ultrasound(rest);
        }
        if (category.startsWith("예방접종")) {
            return vaccine(rest);
        }
        if (category.equals("상급병실료")) {
            return category + " " + String.join(" ", rest);
        }
        List<String> parts = new ArrayList<>(rest);
        if (parts.size() > 1 && parts.get(parts.size() - 1).equals("일반")) {
            parts.remove(parts.size() - 1);
        }
        return String.join(" · ", parts); // "SARS-CoV-2"처럼 이름 안의 '-'는 살린다
    }

    private static String mri(String category, List<String> rest) {
        String part = afterDash(rest.get(0));
        String name = (category.contains("특수") ? "MRI 특수검사 " + part : withAlias(part) + " MRI");
        if (rest.size() >= 2) {
            String option = rest.get(rest.size() - 1);
            if (option.contains("조영제")) {
                name += " (조영제)";
            } else if (!option.equals("일반")) {
                name += " (" + option + ")";
            }
        }
        return name;
    }

    private static String ultrasound(List<String> rest) {
        String mid = afterDash(rest.get(0));
        if (rest.size() == 1) {
            return mid;
        }
        // "간·담낭·담도·비장·췌장-일반" → "간·담낭·담도·비장·췌장", "하지-정맥" → "하지 정맥"
        String last = rest.get(rest.size() - 1).replaceAll("\\s*-\\s*일반$", "").replace('-', ' ').trim();
        if (last.isEmpty() || last.equals("일반")) {
            return mid;
        }
        if (last.length() == 1) { // Ⅰ, Ⅱ 같은 등급
            return mid + " " + last;
        }
        if (US_QUALIFIERS.contains(last) || mid.contains("임산부")) {
            return mid + " (" + last + ")";
        }
        return last.contains("초음파") ? last : withAlias(last) + " 초음파";
    }

    /** 예방접종료/질병…/제품 → "질병 예방접종 (제품)". 질병이 여러 단계면 ·로 잇는다 */
    private static String vaccine(List<String> rest) {
        if (rest.size() == 1) {
            return rest.get(0) + " 예방접종";
        }
        String disease = String.join("·", rest.subList(0, rest.size() - 1));
        disease = VACCINE_NAMES.getOrDefault(disease, disease);
        return disease + " 예방접종 (" + rest.get(rest.size() - 1) + ")";
    }

    /** "근골격계-슬관절" → "슬관절", "근골격, 연부-관절 초음파" → "관절 초음파" */
    private static String afterDash(String s) {
        int dash = s.lastIndexOf('-');
        return (dash >= 0 && dash < s.length() - 1 ? s.substring(dash + 1) : s).trim();
    }

    private static String withAlias(String part) {
        String renamed = RENAMES.get(part.replace(" ", ""));
        if (renamed != null) {
            return renamed;
        }
        String alias = BODY_PARTS.get(part);
        return alias == null || alias.equals(part) ? part : alias + "(" + part + ")";
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
        for (String word : words(query)) {
            if (out.size() >= 5) {
                continue;
            }
            List<String> alts = new ArrayList<>();
            alts.add(word);
            alts.addAll(SYNONYMS.getOrDefault(word.toLowerCase(Locale.ROOT), List.of()));
            alts.addAll(bodyPartTerms(word));
            out.add(alts.stream().distinct().toList());
        }
        return out;
    }

    /**
     * 화면 부위 이름 → 항목명에 쓰인 의학 용어. "무릎" → 슬관절, "다리" → 하지.
     * 항목명엔 의학 용어만 있어서 이게 없으면 "무릎 MRI"가 0건
     */
    private static List<String> bodyPartTerms(String word) {
        List<String> out = new ArrayList<>();
        BODY_PARTS.forEach((term, plain) -> {
            if (plain.equals(word) && !term.equals(word)) {
                out.add(term);
            }
        });
        if (word.equals("다리")) {
            out.add("하지");
        } else if (word.equals("팔")) {
            out.add("상지");
        }
        return out.stream().sorted().toList();
    }

    /** 공백으로 나누고, 붙여 쓴 "…백신"·"…예방접종"은 두 단어로 */
    private static List<String> words(String query) {
        List<String> out = new ArrayList<>();
        for (String word : query.trim().split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            String tail = TRAILING_WORDS.stream()
                    .filter(t -> word.length() > t.length() + 1 && word.endsWith(t))
                    .findFirst().orElse(null);
            if (tail == null) {
                out.add(word);
            } else {
                out.add(word.substring(0, word.length() - tail.length()));
                out.add(tail);
            }
        }
        return out;
    }

    /** 검색어 단어들에 걸린 제외어 (하나라도 들어간 항목은 뺀다) */
    static List<String> excludes(String query) {
        if (query == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String word : words(query)) {
            out.addAll(EXCLUDES.getOrDefault(word.toLowerCase(Locale.ROOT), List.of()));
        }
        return out.stream().distinct().toList();
    }

    /** ILIKE 패턴: %, _ 같은 특수문자는 글자 그대로 찾게 이스케이프 */
    static String like(String s) {
        return "%" + escape(s) + "%";
    }

    /** LIKE 앞부분 일치 패턴 */
    static String prefix(String s) {
        return escape(s) + "%";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
