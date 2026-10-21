package com.medinfo.er.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayNamesTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "재단법인예수병원유지재단예수병원 | 예수병원",
            "의료법인 성수의료재단 인천백병원 | 인천백병원",
            "(의)내경의료재단울산제일병원 | 울산제일병원",
            "학교법인을지학원대전을지대학교병원 | 대전을지대학교병원",
            "재단법인 아산사회복지재단 부속 보령아산병원 | 보령아산병원",
            "의료법인자인의료재단(더자인병원) | 더자인병원",
            "동남권원자력의학원 | 동남권원자력의학원",
            "가톨릭대학교 성빈센트병원 | 가톨릭대학교 성빈센트병원",
            "강북삼성병원 | 강북삼성병원",
            "재단 | 재단"
    })
    void 법인명을_떼고_공백은_살린다(String raw, String expected) {
        assertThat(DisplayNames.clean(raw)).isEqualTo(expected);
    }
}
