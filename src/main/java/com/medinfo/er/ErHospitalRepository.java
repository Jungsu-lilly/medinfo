package com.medinfo.er;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ErHospitalRepository {

    /** 매핑 컬럼(hospital_id, match_method, match_score)은 매칭 단계가 관리하므로 건드리지 않는다 */
    private static final String UPSERT_SQL = """
            INSERT INTO er_hospital (hpid, name, emcls_cd, emcls_nm, main_tel, er_tel, addr, location, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?,
                    ST_SetSRID(ST_MakePoint(?::float8, ?::float8), 4326)::geography, now())
            ON CONFLICT (hpid) DO UPDATE SET
                name = EXCLUDED.name,
                emcls_cd = EXCLUDED.emcls_cd,
                emcls_nm = EXCLUDED.emcls_nm,
                main_tel = EXCLUDED.main_tel,
                er_tel = coalesce(EXCLUDED.er_tel, er_hospital.er_tel),
                addr = EXCLUDED.addr,
                location = EXCLUDED.location,
                updated_at = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    public ErHospitalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void upsertAll(List<ErHospitalRow> rows) {
        jdbcTemplate.batchUpdate(UPSERT_SQL, rows.stream()
                .map(r -> new Object[]{r.hpid(), r.name(), r.emclsCd(), r.emclsNm(),
                        r.mainTel(), r.erTel(), r.addr(), r.lon(), r.lat()})
                .toList());
    }

    /** 위치가 비어 있는 응급기관 수 (실시간 수집이 이름만 먼저 넣어둔 상태) */
    public long countWithoutLocation() {
        Long n = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM er_hospital WHERE location IS NULL", Long.class);
        return n == null ? 0 : n;
    }

    public long count() {
        Long n = jdbcTemplate.queryForObject("SELECT count(*) FROM er_hospital", Long.class);
        return n == null ? 0 : n;
    }
}
