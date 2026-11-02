package com.medinfo.nonpay.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VaccineGroupsTest {

    @Test
    void 항목명의_질병_부분으로_묶음을_찾는다() {
        assertThat(VaccineGroups.of("예방접종료/인플루엔자(독감)/지씨플루프리필드시린지주"))
                .map(VaccineGroups.Group::slug).contains("flu");
        assertThat(VaccineGroups.of("예방접종료/홍역/유행성이하선염/풍진/엠엠알II"))
                .map(VaccineGroups.Group::slug).contains("mmr");
        assertThat(VaccineGroups.of("예방접종료/b형헤모필루스인플루엔자/유히브주"))
                .map(VaccineGroups.Group::slug).contains("hib");
    }

    @Test
    void 예방접종이_아니거나_목록에_없는_질병은_묶지_않는다() {
        assertThat(VaccineGroups.of("검체검사료/인플루엔자 A·B 바이러스항원검사[현장검사]")).isEmpty();
        assertThat(VaccineGroups.of("예방접종료/없는질병/어떤백신")).isEmpty();
        assertThat(VaccineGroups.of("예방접종료/대상포진")).isEmpty();
        assertThat(VaccineGroups.of(null)).isEmpty();
    }

    @Test
    void 제품명은_마지막_부분() {
        assertThat(VaccineGroups.product("예방접종료/대상포진/싱그릭스주")).isEqualTo("싱그릭스주");
    }

    @Test
    void 주소_이름은_겹치지_않고_주소_규칙에_맞는다() {
        assertThat(VaccineGroups.ALL).extracting(VaccineGroups.Group::slug).doesNotHaveDuplicates()
                .allMatch(s -> s.matches("[a-z-]{1,30}"));
        assertThat(VaccineGroups.ALL).extracting(VaccineGroups.Group::disease).doesNotHaveDuplicates();
    }
}
