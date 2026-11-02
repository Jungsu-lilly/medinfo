package com.medinfo.er.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErRegionsTest {

    @Test
    void 시도별로_시군구를_묶고_좌표는_소수_5자리() {
        List<ErRegion> regions = ErStatusController.toRegions(List.of(
                new ErStatusRepository.AreaRow("110000", "서울", "110016", "마포구", 37.5563412, 126.9221999),
                new ErStatusRepository.AreaRow("110000", "서울", "110011", "중구", 37.5641, 126.9979),
                new ErStatusRepository.AreaRow("310000", "경기", "310401", "수원장안구", 37.3, 127.0)));

        assertThat(regions).extracting(ErRegion::name).containsExactly("서울", "경기");
        assertThat(regions.get(0).sggu()).extracting(ErRegion.Area::name).containsExactly("마포구", "중구");
        assertThat(regions.get(0).sggu().get(0).lat()).isEqualTo(37.55634);
        assertThat(regions.get(0).sggu().get(0).lng()).isEqualTo(126.9222);
    }
}
