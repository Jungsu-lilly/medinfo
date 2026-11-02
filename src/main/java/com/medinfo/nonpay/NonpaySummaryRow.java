package com.medinfo.nonpay;

import java.time.LocalDate;

/**
 * 비급여 요약 1건 = 병원 × 항목의 최소~최대 가격 (getNonPaymentItemHospList2).
 * 병원 이름·종별·지역도 같이 오므로, 병원정보 동기화보다 먼저 와도 hospital 행을 만들 수 있다.
 */
public record NonpaySummaryRow(
        String ykiho,
        String hospitalName,
        String clCd,
        String clCdNm,
        String sidoCd,
        String sidoNm,
        String sgguCd,
        String sgguNm,
        String urlAddr,
        String npayCd,
        String korNm,
        String mdivCd,
        String mdivNm,
        String sdivCd,
        String sdivNm,
        String dtlDivCd,
        String dtlDivNm,
        LocalDate adtFrDd,
        LocalDate adtEndDd,
        int minPrc,
        int maxPrc
) {
}
