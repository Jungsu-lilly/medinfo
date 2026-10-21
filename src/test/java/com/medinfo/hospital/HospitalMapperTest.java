package com.medinfo.hospital;

import com.medinfo.publicdata.PublicDataResponse;
import com.medinfo.publicdata.PublicDataXmlParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HospitalMapperTest {

    @Test
    void 병원정보_실제응답을_변환한다() {
        PublicDataResponse response = PublicDataXmlParser.parse(sample("hosp_basis.xml"));
        HospitalRow row = HospitalMapper.map(response.items().get(0)).orElseThrow();

        assertThat(response.totalCount()).isEqualTo(79878);
        assertThat(row.name()).isEqualTo("가톨릭대학교 성빈센트병원");
        assertThat(row.clCd()).isEqualTo("01");
        assertThat(row.sgguNm()).isEqualTo("수원팔달구");
        assertThat(row.lon()).isEqualTo(127.0274271); // XPos = 경도
        assertThat(row.lat()).isEqualTo(37.2779855);  // YPos = 위도
        assertThat(row.tel()).isEqualTo("031-1577-8588");
    }

    @Test
    void 좌표가_없거나_한국_밖이면_좌표만_비운다() {
        Map<String, String> item = new HashMap<>(PublicDataXmlParser.parse(sample("hosp_basis.xml")).items().get(0));
        item.put("XPos", "0");
        item.put("YPos", "0");

        HospitalRow row = HospitalMapper.map(item).orElseThrow();

        assertThat(row.lon()).isNull();
        assertThat(row.lat()).isNull();
        assertThat(row.name()).isNotNull();
    }

    @Test
    void 필수값이_없으면_건너뛴다() {
        Map<String, String> item = new HashMap<>(PublicDataXmlParser.parse(sample("hosp_basis.xml")).items().get(0));
        item.remove("sgguCd");

        assertThat(HospitalMapper.map(item)).isEmpty();
    }

    private static byte[] sample(String name) {
        try (InputStream in = HospitalMapperTest.class.getResourceAsStream("/samples/" + name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
