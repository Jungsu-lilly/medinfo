package com.medinfo.er.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class ErStatusRepository {

    /**
     * 위치 기준 가까운 응급실 + 기관별 최신 병상 1건.
     *  - 최신 병상은 LATERAL + (hpid, collected_at DESC) 인덱스로 기관마다 1건만 읽는다.
     *    er_bed_latest 뷰(DISTINCT ON)는 7일치 전체를 정렬해서 요청마다 쓰기엔 무겁다.
     *  - collected_at을 최근 30분으로 묶어 오늘·어제 파티션만 읽게 한다 (파티션 프루닝).
     *    30분 안에 수집된 게 없으면 병상 정보 없음(null)으로 내려간다.
     */
    private static final String SQL = """
            SELECT e.hpid, e.name, e.emcls_nm, coalesce(e.er_tel, e.main_tel) AS tel, e.addr,
                   ST_Y(e.location::geometry) AS lat, ST_X(e.location::geometry) AS lng,
                   ST_Distance(e.location, p.pt) AS dist,
                   e.hospital_id,
                   s.hvec, s.hvs01, s.hvidate, s.collected_at
              FROM (SELECT ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography AS pt) p
              JOIN er_hospital e
                ON e.location IS NOT NULL
               AND ST_DWithin(e.location, p.pt, ?)
              LEFT JOIN LATERAL (
                    SELECT b.hvec, b.hvs01, b.hvidate, b.collected_at
                      FROM er_bed_snapshot b
                     WHERE b.hpid = e.hpid
                       AND b.collected_at > now() - interval '30 minutes'
                     ORDER BY b.collected_at DESC
                     LIMIT 1
              ) s ON true
             ORDER BY dist
             LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public ErStatusRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Row> findNear(double lat, double lng, int radiusMeters, int limit) {
        // ST_MakePoint(경도, 위도) 순서 주의
        return jdbcTemplate.query(SQL, ErStatusRepository::map, lng, lat, radiusMeters, limit);
    }

    public OffsetDateTime lastCollectedAt() {
        return jdbcTemplate.queryForObject(
                "SELECT max(collected_at) FROM er_bed_snapshot WHERE collected_at > now() - interval '30 minutes'",
                OffsetDateTime.class);
    }

    private static Row map(ResultSet rs, int rowNum) throws SQLException {
        return new Row(
                rs.getString("hpid"),
                rs.getString("name"),
                rs.getString("emcls_nm"),
                rs.getString("tel"),
                rs.getString("addr"),
                rs.getDouble("lat"),
                rs.getDouble("lng"),
                rs.getDouble("dist"),
                (Long) rs.getObject("hospital_id", Long.class),
                (Integer) rs.getObject("hvec", Integer.class),
                (Integer) rs.getObject("hvs01", Integer.class),
                rs.getObject("hvidate", OffsetDateTime.class),
                rs.getObject("collected_at", OffsetDateTime.class)
        );
    }

    public record Row(String hpid, String name, String kind, String tel, String addr,
                      double lat, double lng, double distanceMeters, Long hospitalId,
                      Integer hvec, Integer hvs01, OffsetDateTime hvidate, OffsetDateTime collectedAt) {
    }
}
