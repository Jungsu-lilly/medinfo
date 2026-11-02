package com.medinfo.nonpay.api;

import com.medinfo.nonpay.api.ApiModels.ItemGroup;
import com.medinfo.nonpay.api.ApiModels.Stats;
import com.medinfo.nonpay.api.PriceQueryRepository.HospitalItemRaw;
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
}
