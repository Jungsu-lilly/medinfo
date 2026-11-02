package com.medinfo.nonpay;

import com.medinfo.publicdata.PublicDataResponse;
import com.medinfo.publicdata.PublicDataXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NonpayMapperTest {

    @Test
    void 요약_실제응답을_변환한다() {
        PublicDataResponse response = PublicDataXmlParser.parse(sample("nonpay_summary.xml"));
        NonpaySummaryRow row = NonpayMapper.summary(response.items().get(0)).orElseThrow();

        assertThat(row.hospitalName()).isEqualTo("강북삼성병원");
        assertThat(row.clCdNm()).isEqualTo("상급종합");
        assertThat(row.sgguNm()).isEqualTo("종로구");
        assertThat(row.npayCd()).isEqualTo("ABZ010001");
        assertThat(row.korNm()).isEqualTo("상급병실료/1인실");
        assertThat(row.mdivNm()).isEqualTo("상급병실료");
        assertThat(row.adtFrDd()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(row.adtEndDd()).isEqualTo(LocalDate.of(9999, 12, 31));
        assertThat(row.minPrc()).isEqualTo(380000);
        assertThat(row.maxPrc()).isEqualTo(420000);
        assertThat(row.urlAddr()).startsWith("https://");
    }

    /** 상세 샘플은 공공데이터포털 명세의 태그로 만든 것 (실제 응답을 받으면 교체) */
    @Test
    void 상세_응답을_변환한다() {
        PublicDataResponse response = PublicDataXmlParser.parse(sample("nonpay_detail.xml"));
        NonpayDetailRow row = NonpayMapper.detail(response.items().get(0)).orElseThrow();

        assertThat(response.totalCount()).isEqualTo(1277);
        assertThat(row.sno()).isEqualTo(1);
        assertThat(row.npayCd()).isEqualTo("ABZ010001");
        assertThat(row.yadmNpayCdNm()).isEqualTo("1인실 입원료(일반병동)");
        assertThat(row.curAmt()).isEqualTo(400000);
    }

    @Test
    void 최소_최대가_뒤바뀌면_바로잡는다() {
        Map<String, String> item = summaryItem();
        item.put("minPrc", "500000");
        item.put("maxPrc", "300000");

        NonpaySummaryRow row = NonpayMapper.summary(item).orElseThrow();

        assertThat(row.minPrc()).isEqualTo(300000);
        assertThat(row.maxPrc()).isEqualTo(500000);
    }

    @Test
    void 필수값이_없거나_가격이_이상하면_건너뛴다() {
        Map<String, String> noCode = summaryItem();
        noCode.remove("npayCd");
        Map<String, String> badPrice = summaryItem();
        badPrice.put("minPrc", "가격문의");

        assertThat(NonpayMapper.summary(noCode)).isEmpty();
        assertThat(NonpayMapper.summary(badPrice)).isEmpty();
    }

    @Test
    void 적용종료일이_없으면_현재_적용중으로_본다() {
        Map<String, String> item = summaryItem();
        item.remove("adtEndDd");

        assertThat(NonpayMapper.summary(item).orElseThrow().adtEndDd()).isEqualTo(LocalDate.of(9999, 12, 31));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "380000 | 380000",
            "380000.0 | 380000",
            "65000.5 | 65001",
            "1,200,000 | 1200000",
            "-1 | null",
            "abc | null",
            "99999999999 | null"
    })
    void 금액_변환(String raw, Integer expected) {
        assertThat(NonpayMapper.won(raw)).isEqualTo(expected);
    }

    private static Map<String, String> summaryItem() {
        return new HashMap<>(PublicDataXmlParser.parse(sample("nonpay_summary.xml")).items().get(0));
    }

    private static byte[] sample(String name) {
        try (InputStream in = NonpayMapperTest.class.getResourceAsStream("/samples/" + name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
