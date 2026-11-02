package com.medinfo.nonpay.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemNamesTest {

    @Test
    void 슬래시_뒤가_항목_이름() {
        assertThat(ItemNames.shortName("상급병실료/1인실")).isEqualTo("1인실");
        assertThat(ItemNames.shortName("검체검사료/인플루엔자 A·B 바이러스항원검사[현장검사]"))
                .isEqualTo("인플루엔자 A·B 바이러스항원검사[현장검사]");
        assertThat(ItemNames.shortName("제증명수수료")).isEqualTo("제증명수수료");
        assertThat(ItemNames.shortName("이상한/")).isEqualTo("이상한/");
    }

    @Test
    void 분류_경로는_빈값과_중복을_뺀다() {
        assertThat(ItemNames.path("상급병실료", "1인실", "1인실")).containsExactly("상급병실료", "1인실");
        assertThat(ItemNames.path(null, " ", "기타")).containsExactly("기타");
    }

    @Test
    void 검색어는_단어별로_나누고_동의어를_붙인다() {
        assertThat(ItemNames.terms("무릎 MRI")).containsExactly(List.of("무릎"), List.of("MRI", "자기공명"));
        assertThat(ItemNames.terms("독감")).containsExactly(List.of("독감", "인플루엔자"));
        assertThat(ItemNames.terms("  ")).isEmpty();
        assertThat(ItemNames.terms("a b c d e f g")).hasSize(5);
    }

    @Test
    void LIKE_특수문자는_글자_그대로() {
        assertThat(ItemNames.like("50%")).isEqualTo("%50\\%%");
        assertThat(ItemNames.like("a_b")).isEqualTo("%a\\_b%");
    }
}
