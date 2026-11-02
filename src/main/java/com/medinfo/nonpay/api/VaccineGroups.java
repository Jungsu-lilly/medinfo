package com.medinfo.nonpay.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 예방접종 질병별 묶음. 심평원 항목은 백신 제품마다 따로라(지씨플루, 스카이셀플루 …)
 * 같은 질병 제품을 합쳐 "독감 예방접종 어디가 싸?"에 답한다.
 * 항목명 "예방접종료/{질병}/{제품}"의 질병 부분으로 묶는다. 목록에 없는 질병은 제품별로만 보인다.
 */
final class VaccineGroups {

    static final String CATEGORY = "예방접종료";

    /** slug = 주소(/vaccine/{slug}), label = 화면 이름, disease = 항목명의 질병 부분 그대로 */
    record Group(String slug, String label, String disease) {

        /** nonpay_item.kor_nm LIKE 패턴 (질병 뒤 '/'까지 붙여 "B형간염"이 "B형간염xx"와 섞이지 않게) */
        String korNmPrefix() {
            return CATEGORY + "/" + disease + "/";
        }
    }

    static final List<Group> ALL = List.of(
            new Group("flu", "독감 예방접종", "인플루엔자(독감)"),
            new Group("shingles", "대상포진 예방접종", "대상포진"),
            new Group("hpv", "HPV(자궁경부암) 예방접종", "사람유두종바이러스 감염증(HPV 백신)"),
            new Group("pneumococcal", "폐렴구균 예방접종", "폐렴구균"),
            new Group("hepatitis-a", "A형간염 예방접종", "A형간염"),
            new Group("hepatitis-b", "B형간염 예방접종", "B형간염"),
            new Group("tdap", "Tdap(파상풍·디프테리아·백일해) 예방접종", "Tdap(파상풍, 디프테리아, 백일해)"),
            new Group("td", "Td(파상풍·디프테리아) 예방접종", "Td(파상풍, 디프테리아)"),
            new Group("mmr", "MMR(홍역·볼거리·풍진) 예방접종", "홍역/유행성이하선염/풍진"),
            new Group("varicella", "수두 예방접종", "수두"),
            new Group("meningococcal", "수막구균 예방접종", "수막구균"),
            new Group("hib", "Hib(b형헤모필루스인플루엔자) 예방접종", "b형헤모필루스인플루엔자"));

    private VaccineGroups() {
    }

    static Optional<Group> bySlug(String slug) {
        return ALL.stream().filter(g -> g.slug().equals(slug)).findFirst();
    }

    /** 항목명이 어느 묶음에 속하는지. 예방접종이 아니거나 목록에 없는 질병이면 empty */
    static Optional<Group> of(String korNm) {
        List<String> segs = segments(korNm);
        if (segs.size() < 3 || !segs.get(0).equals(CATEGORY)) {
            return Optional.empty();
        }
        String disease = String.join("/", segs.subList(1, segs.size() - 1));
        return ALL.stream().filter(g -> g.disease().equals(disease)).findFirst();
    }

    /** "예방접종료/인플루엔자(독감)/지씨플루프리필드시린지주" → "지씨플루프리필드시린지주" */
    static String product(String korNm) {
        List<String> segs = segments(korNm);
        return segs.isEmpty() ? korNm : segs.get(segs.size() - 1);
    }

    private static List<String> segments(String korNm) {
        List<String> out = new ArrayList<>();
        if (korNm == null) {
            return out;
        }
        for (String p : korNm.split("/")) {
            if (!p.isBlank()) {
                out.add(p.trim());
            }
        }
        return out;
    }
}
