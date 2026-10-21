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
 * 응급기관(hpid, 국립중앙의료원) ↔ 심평원 병원(ykiho) 자동 매칭. 두 기관이 다른 ID를 써서 위치와 이름으로 짝을 짓는다.
 * 이름은 norm_hosp_name()으로 법인명·괄호·공백·"부속"을 뗀 뒤 비교한다.
 *
 * 0단계 manual: er_manual_match 목록(저장소의 R__er_manual_match.sql)대로 지정. 사람이 정한 것이 항상 우선
 * 3단계 (앞 단계에서 못 찾은 것만 다음 단계로)
 *  1) auto      : 반경 300m 안, 이름 유사도(pg_trgm) 0.5 이상 중 최고
 *  2) auto_far  : 반경 2km 안, 정리된 이름이 완전히 같은 병원 중 가장 가까운 곳
 *                 (두 기관의 좌표 기준점이 달라 같은 병원이 360~1,500m 떨어진 경우)
 *  3) auto_cont : 반경 100m 안, 한쪽 이름이 다른 쪽을 포함 (짧은 쪽 3글자 이상)
 *                 (벌교삼호병원 ↔ 삼호병원, 순천한국병원 ↔ 한국병원처럼 지역명 유무만 다른 경우)
 *  - 매번 전체를 초기화하고 다시 계산한다. 수동 지정의 원본은 DB가 아니라 er_manual_match 목록
 *  - 정리된 이름이 빈 문자열이면 어느 단계에서도 매칭하지 않는다 (빈 이름끼리 "완전일치" 방지)
 *
 * 근거 (2026-10-21 실데이터 530곳): 1단계만으로 516곳, 전부 유사도 1.0. 미매칭 14곳 중
 * 9곳은 이름 완전일치 + 거리 360~1,507m, 3곳은 이름 표기 차이(거리 3~57m), 2곳은 후보 없음(수동 지정 대상).
 *
 * 지표: er_mapping_unmatched (매칭 안 된 응급기관 수) → 갑자기 늘면 데이터 형식 변경 의심
 */
@Component
public class ErHospitalMatcher {

    private static final Logger log = LoggerFactory.getLogger(ErHospitalMatcher.class);

    private static final String RESET_SQL = """
            UPDATE er_hospital
               SET hospital_id = NULL, match_method = NULL, match_score = NULL
             WHERE match_method IS NOT NULL
            """;

    /** 0단계: 수동 목록 */
    private static final String MANUAL_SQL = """
            UPDATE er_hospital e
               SET hospital_id = h.id, match_method = 'manual', match_score = NULL
              FROM er_manual_match m
              JOIN hospital h ON h.ykiho = m.ykiho
             WHERE e.hpid = m.hpid
            """;

    /** 수동 목록인데 병원(ykiho)이나 응급기관(hpid)을 못 찾은 항목 → 오타·폐업 확인용 */
    private static final String MANUAL_BROKEN_SQL = """
            SELECT m.hpid || ' → ' || m.ykiho
              FROM er_manual_match m
              LEFT JOIN er_hospital e ON e.hpid = m.hpid
              LEFT JOIN hospital h ON h.ykiho = m.ykiho
             WHERE e.hpid IS NULL OR h.id IS NULL
            """;

    /** 1단계: 가까운 반경 + 이름 유사도 */
    private static final String STAGE1_SQL = """
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
                       AND norm_hosp_name(e2.name) <> ''
                   ) m
             WHERE e.hpid = m.hpid
               AND m.score >= ?
            """;

    /** 2단계: 넓은 반경 + 정리된 이름 완전일치 */
    private static final String STAGE2_SQL = """
            UPDATE er_hospital e
               SET hospital_id = m.id, match_method = 'auto_far', match_score = 1
              FROM (
                    SELECT e2.hpid, c.id
                      FROM er_hospital e2
                      CROSS JOIN LATERAL (
                            SELECT h.id
                              FROM hospital h
                             WHERE h.location IS NOT NULL
                               AND ST_DWithin(h.location, e2.location, ?)
                               AND norm_hosp_name(h.name) = norm_hosp_name(e2.name)
                             ORDER BY ST_Distance(h.location, e2.location)
                             LIMIT 1
                      ) c
                     WHERE e2.location IS NOT NULL
                       AND e2.match_method IS NULL
                       AND norm_hosp_name(e2.name) <> ''
                   ) m
             WHERE e.hpid = m.hpid
            """;

    /** 3단계: 아주 가까운 반경 + 이름 포함관계 */
    private static final String STAGE3_SQL = """
            UPDATE er_hospital e
               SET hospital_id = m.id, match_method = 'auto_cont', match_score = m.score
              FROM (
                    SELECT e2.hpid, c.id, c.score
                      FROM er_hospital e2
                      CROSS JOIN LATERAL (
                            SELECT h.id,
                                   similarity(norm_hosp_name(e2.name), norm_hosp_name(h.name)) AS score
                              FROM hospital h
                             WHERE h.location IS NOT NULL
                               AND ST_DWithin(h.location, e2.location, ?)
                               AND least(length(norm_hosp_name(h.name)), length(norm_hosp_name(e2.name))) >= 3
                               AND (strpos(norm_hosp_name(e2.name), norm_hosp_name(h.name)) > 0
                                    OR strpos(norm_hosp_name(h.name), norm_hosp_name(e2.name)) > 0)
                             ORDER BY ST_Distance(h.location, e2.location)
                             LIMIT 1
                      ) c
                     WHERE e2.location IS NOT NULL
                       AND e2.match_method IS NULL
                       AND norm_hosp_name(e2.name) <> ''
                   ) m
             WHERE e.hpid = m.hpid
            """;

    private static final String UNMATCHED_SQL = """
            SELECT name FROM er_hospital WHERE hospital_id IS NULL ORDER BY name LIMIT 30
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MatchRules rules;
    private final AtomicLong unmatched = new AtomicLong();

    public ErHospitalMatcher(JdbcTemplate jdbcTemplate, MeterRegistry registry,
                             @Value("${collector.er-match.radius-meters:300}") double radiusMeters,
                             @Value("${collector.er-match.min-similarity:0.5}") double minSimilarity,
                             @Value("${collector.er-match.exact-name-radius-meters:2000}") double exactNameRadius,
                             @Value("${collector.er-match.contains-radius-meters:100}") double containsRadius) {
        this.jdbcTemplate = jdbcTemplate;
        this.rules = new MatchRules(radiusMeters, minSimilarity, exactNameRadius, containsRadius);
        Gauge.builder("er.mapping.unmatched", unmatched, AtomicLong::get).register(registry);
    }

    @Transactional
    public int match() {
        jdbcTemplate.update(RESET_SQL);
        int s0 = jdbcTemplate.update(MANUAL_SQL);
        List<String> broken = jdbcTemplate.queryForList(MANUAL_BROKEN_SQL, String.class);
        if (!broken.isEmpty()) {
            log.warn("수동 매칭 목록 중 적용 못 한 항목 (hpid 또는 ykiho 확인): {}", broken);
        }
        int s1 = jdbcTemplate.update(STAGE1_SQL, rules.radiusMeters(), rules.minSimilarity());
        int s2 = jdbcTemplate.update(STAGE2_SQL, rules.exactNameRadius());
        int s3 = jdbcTemplate.update(STAGE3_SQL, rules.containsRadius());

        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM er_hospital", Long.class);
        Long none = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM er_hospital WHERE hospital_id IS NULL", Long.class);
        unmatched.set(none == null ? 0 : none);

        log.info("응급기관 매칭: 1단계 {} · 2단계(이름 일치, 2km) {} · 3단계(이름 포함, 100m) {} · 수동 {} · 미매칭 {} / 전체 {}",
                s1, s2, s3, s0, unmatched.get(), total);
        if (unmatched.get() > 0) {
            List<String> names = jdbcTemplate.queryForList(UNMATCHED_SQL, String.class);
            log.info("미매칭 응급기관(최대 30곳, 수동 지정 검토용): {}", names);
        }
        return s0 + s1 + s2 + s3;
    }

    record MatchRules(double radiusMeters, double minSimilarity, double exactNameRadius, double containsRadius) {
    }
}
