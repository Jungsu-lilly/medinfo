package com.medinfo.er;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErBedMapperTest {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    @Test
    void 실시간_병상_항목을_변환한다() {
        ErBedRow row = ErBedMapper.map(Map.of(
                "hpid", "A2200001",
                "dutyName", "연세대학교원주세브란스기독병원",
                "dutyTel3", "033-741-0114",
                "hvidate", "20261019214002",
                "hvec", "16",
                "hvs01", "29",
                "hvventisoayn", "N1"
        )).orElseThrow();

        assertThat(row.hvidate()).isEqualTo(OffsetDateTime.of(2026, 10, 19, 21, 40, 2, 0, KST));
        assertThat(row.hvec()).isEqualTo(16);
        assertThat(row.hvs01()).isEqualTo(29);
        assertThat(row.hvicc()).isNull(); // 응답에 없는 필드
    }

    @Test
    void 숫자가_아니거나_음수인_값을_처리한다() {
        assertThat(ErBedMapper.intOrNull("N1")).isNull();
        assertThat(ErBedMapper.intOrNull("")).isNull();
        assertThat(ErBedMapper.intOrNull("-3")).isEqualTo(-3); // 수용 초과: 그대로 저장, 표시는 화면에서
    }

    @Test
    void 필수값이_없으면_건너뛴다() {
        assertThat(ErBedMapper.map(Map.of("hpid", "A1", "dutyName", "병원"))).isEmpty();          // hvidate 없음
        assertThat(ErBedMapper.map(Map.of("dutyName", "병원", "hvidate", "20261019214002"))).isEmpty(); // hpid 없음
    }

    @Test
    void 신선도는_입력시각_지연의_중앙값이다() {
        OffsetDateTime collectedAt = OffsetDateTime.of(2026, 10, 19, 21, 42, 0, 0, KST);
        List<ErBedRow> rows = List.of(
                row(collectedAt.minusSeconds(60)),
                row(collectedAt.minusSeconds(120)),
                row(collectedAt.minusSeconds(3600)) // 한 곳이 1시간째 미갱신이어도 중앙값은 흔들리지 않음
        );

        assertThat(ErBedCollector.medianStalenessSeconds(rows, collectedAt)).isEqualTo(120);
    }

    private static ErBedRow row(OffsetDateTime hvidate) {
        return new ErBedRow("A", "병원", null, hvidate, 1, 1, null, null, null);
    }
}
