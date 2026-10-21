package com.medinfo.publicdata;

import java.util.List;
import java.util.Map;

/**
 * 공공데이터 공통 응답. 오퍼레이션마다 item 필드가 달라서 item은 "태그명 → 값" 맵으로 둔다.
 * 타입 변환은 각 도메인(er, nonpay ...)의 매퍼가 담당.
 */
public record PublicDataResponse(
        List<Map<String, String>> items,
        int totalCount,
        int pageNo,
        int numOfRows
) {
    /** 이번 페이지로 전체를 다 받았는지 (응급실 1회 호출 전국 수집 검증용) */
    public boolean isComplete() {
        return (long) pageNo * numOfRows >= totalCount;
    }
}
