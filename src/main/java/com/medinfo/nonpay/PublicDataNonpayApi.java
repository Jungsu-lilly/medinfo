package com.medinfo.nonpay;

import com.medinfo.publicdata.ApiOperation;
import com.medinfo.publicdata.PublicDataClient;
import com.medinfo.publicdata.PublicDataResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PublicDataNonpayApi implements NonpayApi {

    private final PublicDataClient client;

    public PublicDataNonpayApi(PublicDataClient client) {
        this.client = client;
    }

    /** 지역 조건 없이 전국을 페이지 순서대로 받는다 (전국 약 19.4만 건 → 약 195회) */
    @Override
    public PublicDataResponse summaryPage(int page) {
        return client.call(ApiOperation.NONPAY_SUMMARY,
                Map.of("pageNo", String.valueOf(page), "numOfRows", String.valueOf(PAGE_SIZE)));
    }

    @Override
    public PublicDataResponse detailPage(String ykiho, int page) {
        return client.call(ApiOperation.NONPAY_DETAIL,
                Map.of("ykiho", ykiho, "pageNo", String.valueOf(page), "numOfRows", String.valueOf(PAGE_SIZE)));
    }
}
