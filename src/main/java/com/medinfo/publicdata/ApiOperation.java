package com.medinfo.publicdata;

/**
 * 호출하는 공공데이터 오퍼레이션 목록.
 * 일일 한도가 오퍼레이션 단위라서 한도도 여기서 관리한다 (2026-10-19 활용신청 화면 기준).
 *
 * 주의: 브라우저 주소창에 보이는 "/004046165486/openapi/service/...&SG_APIM=..." 는
 * 게이트웨이 리다이렉트 후 주소라 직접 호출하면 에러 12. 항상 아래 원 경로를 쓴다.
 */
public enum ApiOperation {

    ER_REALTIME_BEDS("/B552657/ErmctInfoInqireService/getEmrrmRltmUsefulSckbdInfoInqire", 1_000),
    ER_LIST("/B552657/ErmctInfoInqireService/getEgytListInfoInqire", 1_000),
    NONPAY_SUMMARY("/B551182/nonPaymentDamtInfoService/getNonPaymentItemHospList2", 10_000),
    NONPAY_DETAIL("/B551182/nonPaymentDamtInfoService/getNonPaymentItemHospDtlList", 10_000),
    HOSP_BASIS("/B551182/hospInfoServicev2/getHospBasisList", 10_000);

    private final String path;
    private final int dailyLimit;

    ApiOperation(String path, int dailyLimit) {
        this.path = path;
        this.dailyLimit = dailyLimit;
    }

    public String path() {
        return path;
    }

    public int dailyLimit() {
        return dailyLimit;
    }
}
