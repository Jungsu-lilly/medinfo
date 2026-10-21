package com.medinfo.publicdata;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 2026-10-19 실제 호출 응답(일부)으로 만든 샘플 기준 */
class PublicDataXmlParserTest {

    @Test
    void 응급실_정상응답을_파싱한다() {
        PublicDataResponse response = PublicDataXmlParser.parse(sample("er_realtime_beds.xml"));

        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.items()).hasSize(3);
        assertThat(response.isComplete()).isTrue();

        Map<String, String> first = response.items().get(0);
        assertThat(first.get("hpid")).isEqualTo("A2200008");
        assertThat(first.get("dutyName")).isEqualTo("강릉아산병원");
        assertThat(first.get("hvec")).isEqualTo("0");
    }

    @Test
    void 비급여_요약_응답의_분류필드와_전체건수를_읽는다() {
        PublicDataResponse response = PublicDataXmlParser.parse(sample("nonpay_summary.xml"));

        assertThat(response.totalCount()).isEqualTo(33078);
        assertThat(response.isComplete()).isFalse();
        assertThat(response.items().get(0))
                .containsEntry("npayMdivCdNm", "상급병실료")
                .containsEntry("minPrc", "380000");
    }

    @Test
    void 게이트웨이_에러는_코드와_함께_예외로_바꾼다() {
        assertThatThrownBy(() -> PublicDataXmlParser.parse(sample("gateway_error.xml")))
                .isInstanceOf(PublicApiException.class)
                .satisfies(e -> {
                    PublicApiException ex = (PublicApiException) e;
                    assertThat(ex.code()).isEqualTo("12");
                    assertThat(ex.retryable()).isFalse();
                });
    }

    @Test
    void DOCTYPE이_있는_XML은_거부한다() {
        byte[] xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <response><header><resultCode>00</resultCode></header></response>
                """.getBytes();

        assertThatThrownBy(() -> PublicDataXmlParser.parse(xxe))
                .isInstanceOf(PublicApiException.class)
                .hasMessageContaining("PARSE");
    }

    private static byte[] sample(String name) {
        try (InputStream in = PublicDataXmlParserTest.class.getResourceAsStream("/samples/" + name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
