package com.medinfo.er;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class ErBedRepository {

    /**
     * 응급기관 목록 배치(erHospitalSyncJob)가 아직 안 돌았어도 스냅샷 FK가 깨지지 않게
     * 실시간 응답에 있는 최소 정보(이름·응급실 전화)로 먼저 등록해 둔다.
     * 주소·좌표·매핑은 목록 배치가 채운다.
     */
    private static final String UPSERT_HOSPITAL_SQL = """
            INSERT INTO er_hospital (hpid, name, er_tel, updated_at)
            VALUES (?, ?, ?, now())
            ON CONFLICT (hpid) DO UPDATE
               SET name = EXCLUDED.name,
                   er_tel = EXCLUDED.er_tel,
                   updated_at = now()
            """;

    private static final String INSERT_SNAPSHOT_SQL = """
            INSERT INTO er_bed_snapshot (hpid, collected_at, hvidate, hvec, hvs01, hvoc, hvgc, hvicc)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public ErBedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void saveSnapshot(List<ErBedRow> rows, OffsetDateTime collectedAt) {
        jdbcTemplate.batchUpdate(UPSERT_HOSPITAL_SQL, rows.stream()
                .map(r -> new Object[]{r.hpid(), r.name(), r.erTel()})
                .toList());

        jdbcTemplate.batchUpdate(INSERT_SNAPSHOT_SQL, rows.stream()
                .map(r -> new Object[]{r.hpid(), collectedAt, r.hvidate(),
                        r.hvec(), r.hvs01(), r.hvoc(), r.hvgc(), r.hvicc()})
                .toList());
    }
}
