package com.medinfo.area;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AreaApiControllerTest {

    @Test
    void 시도별로_시군구를_묶고_좌표는_소수_5자리() {
        List<Area.Sido> areas = AreaApiController.group(List.of(
                new AreaRepository.Row("110000", "서울", "110009", "마포구", 37.5549112, 126.9288611),
                new AreaRepository.Row("110000", "서울", "110011", "중구", 37.5641, 126.9979),
                new AreaRepository.Row("310000", "경기", "310401", "수원장안구", 37.3, 127.0)));

        assertThat(areas).extracting(Area.Sido::name).containsExactly("서울", "경기");
        assertThat(areas.get(0).sggu()).extracting(Area.Sggu::name).containsExactly("마포구", "중구");
        assertThat(areas.get(0).sggu().get(0).lat()).isEqualTo(37.55491);
        assertThat(areas.get(0).sggu().get(0).lng()).isEqualTo(126.92886);
    }
}
