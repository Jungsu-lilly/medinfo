package com.medinfo.nonpay.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemNamesTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "자기공명영상진단료(MRI-기본검사)/근골격계-슬관절/일반 | 무릎(슬관절) MRI",
            "자기공명영상진단료(MRI-기본검사)/척추-요천추/조영제 주입 전·후 촬영 판독 | 허리(요천추) MRI (조영제)",
            "자기공명영상진단료(MRI-기본검사)/뇌/일반 | 뇌 MRI",
            "자기공명영상진단료(MRI-기본검사)/근골격계-관절외 하지/일반 | 다리(관절 외) MRI",
            "자기공명영상진단료(MRI-기본검사)/근골격계-관절외 상지/일반 | 팔(관절 외) MRI",
            "자기공명영상진단료(MRI-특수검사)/확산 | MRI 특수검사 확산",
            "초음파검사료(진단초음파)/두경부-경부 초음파/갑상선·부갑상선 | 갑상선·부갑상선 초음파",
            "초음파검사료(진단초음파)/근골격, 연부-관절 초음파/견관절 | 어깨(견관절) 초음파",
            "초음파검사료(진단초음파)/근골격, 연부-연부조직 초음파/일반 | 연부조직 초음파",
            "초음파검사료(진단초음파)/근골격, 연부-연부조직 초음파/정밀 | 연부조직 초음파 (정밀)",
            "초음파검사료(기본초음파)/단순초음파/Ⅱ | 단순초음파 Ⅱ",
            "초음파검사료(진단초음파)/복부-복부 초음파/간·담낭·담도·비장·췌장-일반 | 간·담낭·담도·비장·췌장 초음파",
            "초음파검사료(진단초음파)/혈관-사지혈관 도플러 초음파/하지-정맥 | 하지 정맥 초음파",
            "초음파검사료(진단초음파)/임산부 초음파/제1삼분기 -일반 | 임산부 초음파 (제1삼분기)",
            "예방접종료/대상포진/싱그릭스주 | 대상포진 예방접종 (싱그릭스주)",
            "예방접종료/홍역/유행성이하선염/풍진/엠엠알II | 홍역·유행성이하선염·풍진 예방접종 (엠엠알II)",
            "예방접종료/사람유두종바이러스 감염증(HPV 백신)/가다실9프리필드시린지 | HPV(자궁경부암) 예방접종 (가다실9프리필드시린지)",
            "상급병실료/1인실 | 상급병실료 1인실",
            "제증명수수료/진단서/일반 | 진단서",
            "검체검사료/인플루엔자 A·B 바이러스 항원과 SARS-CoV-2 항원 동시검사 | 인플루엔자 A·B 바이러스 항원과 SARS-CoV-2 항원 동시검사",
            "제증명수수료 | 제증명수수료"
    })
    void 심평원_항목명을_화면_이름으로(String korNm, String expected) {
        assertThat(ItemNames.shortName(korNm)).isEqualTo(expected);
    }

    @Test
    void 분류_경로는_빈값과_중복을_뺀다() {
        assertThat(ItemNames.path("상급병실료", "1인실", "1인실")).containsExactly("상급병실료", "1인실");
        assertThat(ItemNames.path(null, " ", "기타")).containsExactly("기타");
    }

    @Test
    void 검색어는_단어별로_나누고_동의어를_붙인다() {
        assertThat(ItemNames.terms("무릎 MRI")).containsExactly(List.of("무릎", "슬관절"), List.of("MRI", "자기공명"));
        assertThat(ItemNames.terms("독감")).containsExactly(List.of("독감", "인플루엔자"));
        // 화면에 쓰는 부위 이름으로도 찾는다 (항목명엔 의학 용어만 있음)
        assertThat(ItemNames.terms("무릎 MRI")).first().isEqualTo(List.of("무릎", "슬관절"));
        assertThat(ItemNames.terms("손목")).containsExactly(List.of("손목", "손목관절", "수관절"));
        assertThat(ItemNames.terms("다리")).containsExactly(List.of("다리", "하지"));
        // 화면 이름엔 있지만 항목명엔 없는 말 (HPV(자궁경부암), MMR(홍역·볼거리·풍진))
        assertThat(ItemNames.terms("자궁경부암")).containsExactly(List.of("자궁경부암", "사람유두종바이러스", "자궁경부"));
        assertThat(ItemNames.terms("볼거리")).containsExactly(List.of("볼거리", "유행성이하선염"));
        assertThat(ItemNames.terms("엠알아이")).containsExactly(List.of("엠알아이", "MRI", "자기공명"));
        // 붙여 쓴 "…백신"은 두 단어로
        assertThat(ItemNames.terms("자궁경부암백신")).containsExactly(
                List.of("자궁경부암", "사람유두종바이러스", "자궁경부"), List.of("백신", "예방접종"));
        assertThat(ItemNames.terms("백신")).containsExactly(List.of("백신", "예방접종"));
        assertThat(ItemNames.terms("예방접종")).containsExactly(List.of("예방접종"));
        assertThat(ItemNames.excludes("독감백신")).containsExactly("헤모필루스");
        assertThat(ItemNames.terms("  ")).isEmpty();
        assertThat(ItemNames.terms("a b c d e f g")).hasSize(5);
        // 독감으로 찾으면 Hib(b형헤모필루스인플루엔자) 백신은 뺀다
        assertThat(ItemNames.excludes("독감")).containsExactly("헤모필루스");
        assertThat(ItemNames.excludes("독감 예방접종")).containsExactly("헤모필루스");
        assertThat(ItemNames.excludes("인플루엔자")).isEmpty();
        assertThat(ItemNames.prefix("예방접종료/A_형/")).isEqualTo("예방접종료/A\\_형/%");
    }

    @Test
    void LIKE_특수문자는_글자_그대로() {
        assertThat(ItemNames.like("50%")).isEqualTo("%50\\%%");
        assertThat(ItemNames.like("a_b")).isEqualTo("%a\\_b%");
    }
}
