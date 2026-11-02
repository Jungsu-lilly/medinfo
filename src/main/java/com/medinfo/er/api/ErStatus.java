package com.medinfo.er.api;

import java.util.List;

/** GET /api/er 응답 */
public record ErStatus(
        String generatedAt,     // 응답 생성 시각 (ISO-8601, KST)
        String lastCollectedAt, // 가장 최근 수집 시각. null이면 최근 30분간 수집 실패
        List<Item> items
) {

    public record Item(
            String hpid,
            String name,             // 화면 표시용 (법인명 제거)
            String kind,             // 응급의료기관 분류 (권역응급의료센터 등)
            String erTel,
            String addr,
            Double lat,
            Double lng,
            int distanceMeters,
            Long hospitalId,         // 심평원 병원과 매칭됐으면 병원 상세로 연결
            Integer available,       // hvec 응급실 남은 병상 (병상 정보 없으면 null, 수용 초과면 음수 가능)
            Integer total,           // hvs01 응급실 기준 병상
            String inputAt,          // hvidate 병원이 마지막으로 입력한 시각
            Integer minutesSinceInput,
            boolean stale            // 병원 입력이 오래돼 전화 확인이 필요한지
    ) {
    }
}
