package com.medinfo.er;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 응급기관(hpid, 국립중앙의료원) ↔ 심평원 병원(ykiho) 자동 매칭.
 *
 * 두 기관이 서로 다른 ID를 써서, 위치와 이름으로 짝을 짓는다.
 *  1) 응급기관 좌표 반경 radius 안의 병원만 후보 (PostGIS ST_DWithin, GIST 인덱스 사용)
 *  2) 법인명을 뗀 이름(norm_hosp_name)끼리 글자 유사도(pg_trgm similarity)가 가장 높은 후보
 *  3) 유사도가 minSimilarity 이상이면 auto 매칭, 아니면 미매칭(NULL)
 *  - match_method = 'manual' 인 행은 사람이 정한 것이라 절대 덮어쓰지 않는다
 *
 * 기준값 근거(2026-10-21 테스트): 같은 병원 표기 차이는 유사도 1.0, "대전을지대학교병원"과 "충남대학교병원"처럼
 * 다른 병원도 "대학교병원"이 겹쳐 0.29가 나와서 0.5로 둠. 반경은 기관마다 좌표 기준점이 달라 300m.
 *
 * 지표: er_mapping_unmatched (매칭 안 된 응급기관 수) → 갑자기 늘면 데이터 형식 변경 의심
 */
@Component
public class ErHospitalMatcher {

    private static final Logger log = LoggerFactory.getLogger(ErHospitalMatcher.class);

    private static final String RESET_AUTO_SQL = """
            UPDATE er_hospital
               SET hospital_id = NULL, match_method = NULL, match_score = NULL
             WHERE match_method = 'auto'
            """;

    private static final String MATCH_SQL = """
            UPDATE er_hospital e
               SET hospital_id = m.id, match_method = 'auto', match_score = m.score
              FROM (
                    SELECT e2.hpid, c.id, c.score
                      FROM er_hospital e2
                      CROSS JOIN LATERAL (
                            SELECT h.id,
                                   similarity(norm_hosp_name(e2.name), norm_hosp_name(h.name)) AS score
                              FROM hospital h
                             WHERE h.location IS NOT NULL
                               AND ST_DWithin(h.location, e2.location, ?)
                             ORDER BY score DESC, ST_Distance(h.location, e2.location)
                             LIMIT 1
                      ) c
                     WHERE e2.location IS NOT NULL
                       AND e2.match_method IS NULL
                   ) m
             WHERE e.hpid = m.hpid
               AND m.score >= ?
            """;

    private static final String UNMATCHED_SQL = """
            SELECT name FROM er_hospital WHERE hospital_id IS NULL ORDER BY name LIMIT 30
            """;

    private final JdbcTemplate jdbcTemplate;
    private final double radiusMeters;
    private final double minSimilarity;
    private final AtomicLong unmatched = new AtomicLong();

    public ErHospitalMatcher(JdbcTemplate jdbcTemplate, MeterRegistry registry,
                             @Value("${collector.er-match.radius-meters:300}") double radiusMeters,
                             @Value("${collector.er-match.min-similarity:0.5}") double minSimilarity) {
        this.jdbcTemplate = jdbcTemplate;
        this.radiusMeters = radiusMeters;
        this.minSimilarity = minSimilarity;
        Gauge.builder("er.mapping.unmatched", unmatched, AtomicLong::get).register(registry);
    }

    @Transactional
    public int match() {
        jdbcTemplate.update(RESET_AUTO_SQL);
        int matched = jdbcTemplate.update(MATCH_SQL, radiusMeters, minSimilarity);

        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM er_hospital", Long.class);
        Long none = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM er_hospital WHERE hospital_id IS NULL", Long.class);
        unmatched.set(none == null ? 0 : none);

        log.info("응급기관 매칭: 자동 {}곳, 미매칭 {}곳 / 전체 {}곳", matched, unmatched.get(), total);
        if (unmatched.get() > 0) {
            List<String> names = jdbcTemplate.queryForList(UNMATCHED_SQL, String.class);
            log.info("미매칭 응급기관(최대 30곳, 수동 지정 검토용): {}", names);
        }
        return matched;
    }
}
