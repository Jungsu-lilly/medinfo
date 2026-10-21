package com.medinfo.hospital;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class HospitalRepository {

    /**
     * ykiho 기준 upsert. nonpay_url은 비급여 수집이 채우므로 건드리지 않는다.
     * 좌표가 null이면 ST_MakePoint가 null을 돌려줘서 location도 null (PostGIS 함수는 STRICT).
     */
    private static final String UPSERT_SQL = """
            INSERT INTO hospital (ykiho, name, cl_cd, cl_cd_nm, sido_cd, sido_nm, sggu_cd, sggu_nm,
                                  addr, location, tel, hosp_url, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ST_SetSRID(ST_MakePoint(?::float8, ?::float8), 4326)::geography,
                    ?, ?, now())
            ON CONFLICT (ykiho) DO UPDATE SET
                name = EXCLUDED.name,
                cl_cd = EXCLUDED.cl_cd,
                cl_cd_nm = EXCLUDED.cl_cd_nm,
                sido_cd = EXCLUDED.sido_cd,
                sido_nm = EXCLUDED.sido_nm,
                sggu_cd = EXCLUDED.sggu_cd,
                sggu_nm = EXCLUDED.sggu_nm,
                addr = EXCLUDED.addr,
                location = EXCLUDED.location,
                tel = EXCLUDED.tel,
                hosp_url = EXCLUDED.hosp_url,
                updated_at = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    public HospitalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 한 페이지(최대 1000건) 단위로 호출 → 페이지마다 커밋. 중간에 실패해도 앞 페이지는 남는다 */
    @Transactional
    public void upsertAll(List<HospitalRow> rows) {
        jdbcTemplate.batchUpdate(UPSERT_SQL, rows.stream()
                .map(r -> new Object[]{
                        r.ykiho(), r.name(), r.clCd(), r.clCdNm(), r.sidoCd(), r.sidoNm(), r.sgguCd(), r.sgguNm(),
                        r.addr(), r.lon(), r.lat(), r.tel(), r.hospUrl()})
                .toList());
    }

    public long count() {
        Long n = jdbcTemplate.queryForObject("SELECT count(*) FROM hospital", Long.class);
        return n == null ? 0 : n;
    }
}
