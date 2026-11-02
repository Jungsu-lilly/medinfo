package com.medinfo.nonpay;

import java.time.LocalDate;

/**
 * 비급여 상세 1건 = 병원이 자체 이름으로 신고한 세부 항목 하나의 금액 (getNonPaymentItemHospDtlList).
 * 같은 npayCd라도 병원 항목명(yadmNpayCdNm)별로 여러 행이 있어 sno로 구분한다.
 */
public record NonpayDetailRow(
        int sno,
        String npayCd,
        String yadmNpayCdNm,
        LocalDate adtFrDd,
        LocalDate adtEndDd,
        int curAmt
) {
}
