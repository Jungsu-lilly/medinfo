package com.medinfo.er;

import com.medinfo.publicdata.PublicDataResponse;
import com.medinfo.publicdata.PublicDataXmlParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErHospitalMapperTest {

    @Test
    void 응급기관_목록_실제응답을_변환한다() {
        PublicDataResponse response = PublicDataXmlParser.parse(sample("er_list.xml"));
        ErHospitalRow row = ErHospitalMapper.map(response.items().get(0)).orElseThrow();

        assertThat(response.totalCount()).isEqualTo(530);
        assertThat(row.hpid()).isEqualTo("A1700023");
        assertThat(row.name()).isEqualTo("(의)내경의료재단울산제일병원");
        assertThat(row.emclsNm()).isEqualTo("응급실운영신고기관");
        assertThat(row.lon()).isEqualTo(129.30701143429678);
        assertThat(row.lat()).isEqualTo(35.54823820112527);
    }

    @Test
    void hpid가_없으면_건너뛴다() {
        assertThat(ErHospitalMapper.map(Map.of("dutyName", "병원"))).isEmpty();
    }

    private static byte[] sample(String name) {
        try (InputStream in = ErHospitalMapperTest.class.getResourceAsStream("/samples/" + name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
