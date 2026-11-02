package com.medinfo.nonpay.api;

import com.medinfo.nonpay.api.ApiModels.ItemGroup;
import com.medinfo.nonpay.api.ApiModels.ItemSummary;
import com.medinfo.nonpay.api.ApiModels.PriceList;
import com.medinfo.nonpay.api.ApiModels.PriceRow;
import com.medinfo.nonpay.api.ApiModels.Stats;
import com.medinfo.nonpay.api.PriceQueryRepository.HospitalItemRaw;
import com.medinfo.nonpay.api.PriceQueryRepository.ItemStatRow;
import com.medinfo.nonpay.api.PriceQueryRepository.PriceRaw;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceServiceTest {

    @Test
    void 통계는_병원별_최저가_기준() {
        assertThat(PriceService.stats(List.of(300, 100, 200))).isEqualTo(new Stats(3, 100, 200, 300));
        assertThat(PriceService.stats(List.of(100, 200, 300, 400))).isEqualTo(new Stats(4, 100, 250, 400));
        assertThat(PriceService.stats(List.of())).isEqualTo(new Stats(0, 0, 0, 0));
    }

    @Test
    void 중분류별로_묶고_분류없는_항목은_기타로_맨뒤() {
        List<ItemGroup> groups = PriceService.group(List.of(
                new HospitalItemRaw("A1", "1인실 입원료", "상급병실료/1인실", "상급병실료", 380000, 300000, 12),
                new HospitalItemRaw("X9", "알 수 없는 항목", null, null, 1000, null, null),
                new HospitalItemRaw("A2", "2인실 입원료", "상급병실료/2인실", "상급병실료", 200000, 150000, 2),
                new HospitalItemRaw("C1", "Influenza A&B Ag", "검체검사료/인플루엔자", "검체검사료", 65000, 40000, 30)));

        assertThat(groups).extracting(ItemGroup::name).containsExactly("상급병실료", "검체검사료", "기타");
        assertThat(groups.get(0).items()).hasSize(2);
        assertThat(groups.get(0).items().get(0).std()).isEqualTo("상급병실료 1인실");
        assertThat(groups.get(0).items().get(0).regionMedian()).isEqualTo(300000);
        // 같은 시도 병원이 3곳 미만이면 중간값을 보여주지 않는다
        assertThat(groups.get(0).items().get(1).regionMedian()).isNull();
    }

    @Test
    void 같은_예방접종_제품이_2개_이상이면_묶음_카드_하나로() {
        List<ItemSummary> out = PriceService.groupVaccines(List.of(
                stat("CZ3940000", "검체검사료/인플루엔자 A·B 바이러스항원검사[현장검사]", 1930),
                stat("V1", "예방접종료/인플루엔자(독감)/지씨플루프리필드시린지주", 338),
                stat("D66300000", "검체검사료/인플루엔자 A·B 바이러스 항원과 SARS-CoV-2 항원 동시검사", 557),
                stat("V2", "예방접종료/인플루엔자(독감)/스카이셀플루프리필드시린지", 252)),
                g -> new ItemSummary(null, g.label(), g.disease(), "백신 9종", new Stats(800, 10000, 30000, 70000), g.slug()));

        assertThat(out).extracting(ItemSummary::name)
                .containsExactly("인플루엔자 A·B 바이러스항원검사[현장검사]", "독감 예방접종",
                        "인플루엔자 A·B 바이러스 항원과 SARS-CoV-2 항원 동시검사");
        assertThat(out.get(1).vaccine()).isEqualTo("flu");
        assertThat(out.get(1).code()).isNull();
    }

    @Test
    void 예방접종_제품이_하나만_걸리면_그_제품을_그대로() {
        List<ItemSummary> out = PriceService.groupVaccines(List.of(
                stat("3Z5200303", "예방접종료/대상포진/싱그릭스주", 794)),
                g -> { throw new AssertionError("묶지 않아야 함"); });

        assertThat(out).extracting(ItemSummary::code).containsExactly("3Z5200303");
        assertThat(out.get(0).vaccine()).isNull();
    }

    @Test
    void 묶음_가격_목록은_병원마다_가장_싼_제품_이름을_붙인다() {
        VaccineGroups.Group flu = VaccineGroups.bySlug("flu").orElseThrow();
        List<ItemStatRow> all = List.of(
                stat("V1", "예방접종료/인플루엔자(독감)/지씨플루프리필드시린지주", 338),
                stat("V2", "예방접종료/인플루엔자(독감)/스카이셀플루프리필드시린지", 252));
        PriceList list = PriceService.toVaccineList(flu, all, all.subList(0, 1), null, null, List.of(
                new PriceRaw(1, "가병원", "병원", 15000, 20000, null, "V2"),
                new PriceRaw(2, "나병원", "종합병원", 30000, 30000, null, "V1")), "2026-11-01");

        assertThat(list.item().code()).isEqualTo("flu");
        assertThat(list.item().name()).isEqualTo("독감 예방접종");
        assertThat(list.item().path()).containsExactly("예방접종료", "인플루엔자(독감)");
        assertThat(list.rows()).extracting(PriceRow::product)
                .containsExactly("스카이셀플루프리필드시린지", "지씨플루프리필드시린지주");
        assertThat(list.stats()).isEqualTo(new Stats(2, 15000, 22500, 30000));
        assertThat(list.products()).extracting(ItemSummary::name).containsExactly("지씨플루프리필드시린지주");
    }

    private static ItemStatRow stat(String code, String korNm, int hospitals) {
        return new ItemStatRow(code, korNm, korNm.substring(0, korNm.indexOf('/')), hospitals, 10000, 20000, 50000);
    }

    @Test
    void 가격이_없는_이유는_종별로_구분한다() {
        assertThat(PriceService.noPriceReason("의원")).isEqualTo("CLINIC");
        assertThat(PriceService.noPriceReason("치과의원")).isEqualTo("CLINIC");
        assertThat(PriceService.noPriceReason("한의원")).isEqualTo("CLINIC");
        assertThat(PriceService.noPriceReason("보건지소")).isEqualTo("CLINIC");
        assertThat(PriceService.noPriceReason("병원")).isEqualTo("NOT_REPORTED");
        assertThat(PriceService.noPriceReason("치과병원")).isEqualTo("NOT_REPORTED");
        assertThat(PriceService.noPriceReason("요양병원")).isEqualTo("NOT_REPORTED");
    }
}
