package com.medinfo.nonpay;

import com.medinfo.publicdata.PublicDataResponse;

/** 비급여 API 호출 창구. 배치 리더는 이 인터페이스만 알아서 테스트에서 가짜 응답으로 바꿀 수 있다 */
public interface NonpayApi {

    int PAGE_SIZE = 1000; // numOfRows 1000 정상 동작 확인 (2026-10-19)

    /** 전국 요약 page번째 (1부터) */
    PublicDataResponse summaryPage(int page);

    /** 병원 1곳 상세 page번째 (1부터) */
    PublicDataResponse detailPage(String ykiho, int page);
}
