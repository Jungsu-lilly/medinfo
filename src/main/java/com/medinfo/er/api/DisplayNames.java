package com.medinfo.er.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 화면에 보여줄 병원 이름 정리. DB의 norm_hosp_name()과 규칙은 같지만 공백은 살린다.
 *   재단법인예수병원유지재단예수병원          → 예수병원
 *   의료법인 성수의료재단 인천백병원         → 인천백병원
 *   (의)내경의료재단울산제일병원            → 울산제일병원
 *   의료법인자인의료재단(더자인병원)         → 더자인병원   (괄호 안이 진짜 병원 이름인 경우)
 *   동남권원자력의학원                      → 그대로       (이름 자체가 "…학원"으로 끝나는 경우)
 */
public final class DisplayNames {

    private static final Pattern PAREN_NAME = Pattern.compile("^.*\\(([^)]*(?:병원|의원|의료원|센터))\\)\\s*$");
    private static final Pattern SHORT_PREFIX = Pattern.compile("^\\((?:의|재|학|사|복)\\)\\s*");
    private static final Pattern LEGAL_ENTITY = Pattern.compile("(?:의료법인|재단법인|학교법인|사회복지법인|사단법인|특수법인)\\s*");
    private static final Pattern FOUNDATION = Pattern.compile("^.*?(?:유지재단|의료재단|복지재단|재단|학원)\\s*(?=\\S)");
    private static final Pattern ATTACHED = Pattern.compile("^부속\\s*");

    private DisplayNames() {
    }

    public static String clean(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String s = name.trim();

        Matcher paren = PAREN_NAME.matcher(s);
        if (paren.matches()) {
            return paren.group(1).trim();
        }

        s = SHORT_PREFIX.matcher(s).replaceFirst("");
        s = LEGAL_ENTITY.matcher(s).replaceAll("");
        s = FOUNDATION.matcher(s).replaceFirst("");
        s = ATTACHED.matcher(s).replaceFirst("");
        s = s.trim();
        return s.isEmpty() ? name.trim() : s;
    }
}
