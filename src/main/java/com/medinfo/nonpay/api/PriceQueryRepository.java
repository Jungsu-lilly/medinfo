package com.medinfo.nonpay.api;

import com.medinfo.nonpay.api.ApiModels.Stats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 가격·병원 조회 SQL (읽기 전용).
 * 가격은 nonpay_price_current(병원×항목 현재 가격 1건), 통계는 nonpay_price_stats(미리 계산) 사용.
 */
@Repository
public class PriceQueryRepository {

    static final String NATIONAL = "000000";

    private final JdbcTemplate jdbc;

    public PriceQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── 항목 ─────────────────────────────────────────

    public Optional<ItemRow> findItem(String code) {
        List<ItemRow> rows = jdbc.query(
                "SELECT npay_cd, kor_nm, mdiv_nm, sdiv_nm, dtl_div_nm FROM nonpay_item WHERE npay_cd = ?",
                (rs, i) -> new ItemRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                code);
        return rows.stream().findFirst();
    }

    private static final String ITEM_STAT_COLUMNS = """
            SELECT i.npay_cd, i.kor_nm, i.mdiv_nm, s.hospital_count, s.low, s.median, s.high
              FROM nonpay_item i
              JOIN nonpay_price_stats s ON s.npay_cd = i.npay_cd AND s.sido_cd = ?
            """;

    private static final RowMapper<ItemStatRow> ITEM_STAT = (rs, i) -> new ItemStatRow(
            rs.getString("npay_cd"), rs.getString("kor_nm"), rs.getString("mdiv_nm"),
            rs.getInt("hospital_count"), rs.getInt("low"), rs.getInt("median"), rs.getInt("high"));

    /** terms: 단어끼리 AND, 동의어끼리 OR. excludes가 들어간 항목은 뺀다. 이름·분류명 전체에서 찾고 병원 많은 순 */
    public List<ItemStatRow> searchItems(List<List<String>> terms, List<String> excludes, int limit) {
        if (terms.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder(ITEM_STAT_COLUMNS).append(" WHERE true");
        List<Object> args = new ArrayList<>();
        args.add(NATIONAL);
        for (List<String> alts : terms) {
            sql.append(" AND (");
            for (int k = 0; k < alts.size(); k++) {
                if (k > 0) {
                    sql.append(" OR ");
                }
                sql.append("concat_ws(' ', i.kor_nm, i.mdiv_nm, i.sdiv_nm, i.dtl_div_nm) ILIKE ? ESCAPE '\\'");
                args.add(ItemNames.like(alts.get(k)));
            }
            sql.append(")");
        }
        for (String ex : excludes) {
            sql.append(" AND i.kor_nm NOT ILIKE ? ESCAPE '\\'");
            args.add(ItemNames.like(ex));
        }
        sql.append(" ORDER BY s.hospital_count DESC, i.kor_nm LIMIT ?");
        args.add(limit);
        return jdbc.query(sql.toString(), ITEM_STAT, args.toArray());
    }

    /** 지정한 코드들 (순서 유지). 통계가 없는(그 지역에 병원이 없는) 코드는 빠진다 */
    public List<ItemStatRow> itemsByCodes(List<String> codes, String sido) {
        if (codes.isEmpty()) {
            return List.of();
        }
        String sql = ITEM_STAT_COLUMNS + " WHERE i.npay_cd = ANY (?) ";
        List<ItemStatRow> rows = new ArrayList<>(jdbc.query(con -> {
            var ps = con.prepareStatement(sql);
            ps.setString(1, sido);
            ps.setArray(2, con.createArrayOf("varchar", codes.toArray()));
            return ps;
        }, ITEM_STAT));
        rows.sort((a, b) -> Integer.compare(codes.indexOf(a.code()), codes.indexOf(b.code())));
        return rows;
    }

    /** 예방접종 묶음의 제품들 (항목명 앞부분 일치). 그 지역에 가격이 있는 제품만, 병원 많은 순 */
    public List<ItemStatRow> itemsByNamePrefix(String korNmPrefix, String sido) {
        return jdbc.query(ITEM_STAT_COLUMNS + " WHERE i.kor_nm LIKE ? ESCAPE '\\' ORDER BY s.hospital_count DESC, i.kor_nm",
                ITEM_STAT, sido, ItemNames.prefix(korNmPrefix));
    }

    /**
     * 여러 항목을 합친 전국 통계. 병원마다 가장 싼 항목 가격 1개로 세서
     * 묶음 페이지(병원별 최저가 목록)의 통계와 같은 기준이 되게 한다
     */
    public Stats combinedStats(List<String> codes) {
        if (codes.isEmpty()) {
            return new Stats(0, 0, 0, 0);
        }
        return jdbc.query(con -> {
            var ps = con.prepareStatement("""
                    SELECT count(*), coalesce(min(m), 0), coalesce(round(percentile_cont(0.5) WITHIN GROUP (ORDER BY m)), 0),
                           coalesce(max(m), 0)
                      FROM (SELECT hospital_id, min(min_prc) AS m
                              FROM nonpay_price_current
                             WHERE npay_cd = ANY (?)
                             GROUP BY hospital_id) t
                    """);
            ps.setArray(1, con.createArrayOf("varchar", codes.toArray()));
            return ps;
        }, (rs, i) -> new Stats(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4))).get(0);
    }

    public List<ItemStatRow> topItems(String sido, int limit) {
        return jdbc.query(ITEM_STAT_COLUMNS + " ORDER BY s.hospital_count DESC, i.kor_nm LIMIT ?",
                ITEM_STAT, sido, limit);
    }

    // ── 가격 비교 ─────────────────────────────────────

    private static final RowMapper<PriceRaw> PRICE = (rs, i) -> new PriceRaw(
            rs.getLong("id"), rs.getString("name"), rs.getString("cl_cd_nm"),
            rs.getInt("min_prc"), rs.getInt("max_prc"), (Double) rs.getObject("dist", Double.class),
            rs.getString("npay_cd"));

    /**
     * 시도(+시군구) 안의 병원. idx_summary_compare(npay_cd, sido_cd, sggu_cd, min_prc)
     * codes가 여러 개(예방접종 묶음)면 병원마다 가장 싼 항목 1건만
     */
    public List<PriceRaw> pricesInRegion(List<String> codes, String sido, String sggu, int limit) {
        return jdbc.query(con -> {
            var ps = con.prepareStatement("""
                    SELECT * FROM (
                        SELECT DISTINCT ON (h.id) h.id, h.name, h.cl_cd_nm, p.min_prc, p.max_prc, NULL::float8 AS dist, p.npay_cd
                          FROM nonpay_price_current p
                          JOIN hospital h ON h.id = p.hospital_id
                         WHERE p.npay_cd = ANY (?)
                           AND p.sido_cd = ?
                           AND (?::text IS NULL OR p.sggu_cd = ?)
                         ORDER BY h.id, p.min_prc, p.npay_cd
                    ) t
                     ORDER BY min_prc, name
                     LIMIT ?
                    """);
            ps.setArray(1, con.createArrayOf("varchar", codes.toArray()));
            ps.setString(2, sido);
            ps.setString(3, sggu);
            ps.setString(4, sggu);
            ps.setInt(5, limit);
            return ps;
        }, PRICE);
    }

    /** 내 위치 반경 안의 병원 (좌표가 있는 병원만). codes가 여러 개면 병원마다 가장 싼 항목 1건만 */
    public List<PriceRaw> pricesNear(List<String> codes, double lat, double lng, int radiusMeters, int limit) {
        return jdbc.query(con -> {
            var ps = con.prepareStatement("""
                    SELECT * FROM (
                        SELECT DISTINCT ON (h.id) h.id, h.name, h.cl_cd_nm, p.min_prc, p.max_prc,
                               ST_Distance(h.location, pt.g) AS dist, p.npay_cd
                          FROM (SELECT ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography AS g) pt
                          JOIN hospital h ON h.location IS NOT NULL AND ST_DWithin(h.location, pt.g, ?)
                          JOIN nonpay_price_current p ON p.hospital_id = h.id AND p.npay_cd = ANY (?)
                         ORDER BY h.id, p.min_prc, p.npay_cd
                    ) t
                     ORDER BY min_prc, name
                     LIMIT ?
                    """);
            ps.setDouble(1, lng);
            ps.setDouble(2, lat);
            ps.setInt(3, radiusMeters);
            ps.setArray(4, con.createArrayOf("varchar", codes.toArray()));
            ps.setInt(5, limit);
            return ps;
        }, PRICE);
    }

    // ── 병원 ─────────────────────────────────────────

    public Optional<HospitalRow> findHospital(long id) {
        List<HospitalRow> rows = jdbc.query("""
                SELECT id, name, cl_cd_nm, addr, tel, hosp_url, nonpay_url, sido_cd, sido_nm, sggu_nm,
                       ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lng
                  FROM hospital WHERE id = ?
                """, (rs, i) -> new HospitalRow(
                rs.getLong("id"), rs.getString("name"), rs.getString("cl_cd_nm"), rs.getString("addr"),
                rs.getString("tel"), rs.getString("hosp_url"), rs.getString("nonpay_url"),
                rs.getString("sido_cd"), rs.getString("sido_nm"), rs.getString("sggu_nm"),
                (Double) rs.getObject("lat", Double.class), (Double) rs.getObject("lng", Double.class)), id);
        return rows.stream().findFirst();
    }

    private static final RowMapper<HospitalItemRaw> HOSPITAL_ITEM = (rs, i) -> new HospitalItemRaw(
            rs.getString("npay_cd"), rs.getString("item_name"), rs.getString("kor_nm"), rs.getString("mdiv_nm"),
            rs.getInt("price"), (Integer) rs.getObject("median", Integer.class),
            (Integer) rs.getObject("hospital_count", Integer.class));

    /** 병원 상세 항목 (병원 자체 이름 + 단일 금액). 지역 통계는 같은 시도 기준 */
    public List<HospitalItemRaw> hospitalDetailItems(long hospitalId, String sido) {
        return jdbc.query("""
                SELECT d.npay_cd, coalesce(d.yadm_npay_cd_nm, i.kor_nm, d.npay_cd) AS item_name,
                       i.kor_nm, i.mdiv_nm, d.cur_amt AS price, s.median, s.hospital_count
                  FROM nonpay_price_detail d
                  LEFT JOIN nonpay_item i ON i.npay_cd = d.npay_cd
                  LEFT JOIN nonpay_price_stats s ON s.npay_cd = d.npay_cd AND s.sido_cd = ?
                 WHERE d.hospital_id = ?
                   AND d.adt_end_dd >= current_date
                 ORDER BY i.mdiv_nm NULLS LAST, item_name, d.sno
                """, HOSPITAL_ITEM, sido, hospitalId);
    }

    /** 상세가 아직 없을 때(적재 전) 요약으로 대신: 표준 이름 + 병원 최저가 */
    public List<HospitalItemRaw> hospitalSummaryItems(long hospitalId, String sido) {
        return jdbc.query("""
                SELECT p.npay_cd, i.kor_nm AS item_name, i.kor_nm, i.mdiv_nm, p.min_prc AS price,
                       s.median, s.hospital_count
                  FROM nonpay_price_current p
                  JOIN nonpay_item i ON i.npay_cd = p.npay_cd
                  LEFT JOIN nonpay_price_stats s ON s.npay_cd = p.npay_cd AND s.sido_cd = ?
                 WHERE p.hospital_id = ?
                 ORDER BY i.mdiv_nm NULLS LAST, i.kor_nm
                """, HOSPITAL_ITEM, sido, hospitalId);
    }

    private static final RowMapper<HospitalListRaw> HOSPITAL_LIST = (rs, i) -> new HospitalListRaw(
            rs.getLong("id"), rs.getString("name"), rs.getString("cl_cd_nm"), rs.getString("addr"),
            (Double) rs.getObject("dist", Double.class), rs.getBoolean("has_prices"));

    /** 비급여 가격이 있는 병원 중 가까운 순 */
    public List<HospitalListRaw> hospitalsNear(double lat, double lng, int radiusMeters, int limit) {
        return jdbc.query("""
                SELECT h.id, h.name, h.cl_cd_nm, h.addr, ST_Distance(h.location, pt.g) AS dist, true AS has_prices
                  FROM (SELECT ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography AS g) pt
                  JOIN hospital h ON h.location IS NOT NULL AND ST_DWithin(h.location, pt.g, ?)
                 WHERE EXISTS (SELECT 1 FROM nonpay_price_summary s WHERE s.hospital_id = h.id)
                 ORDER BY dist
                 LIMIT ?
                """, HOSPITAL_LIST, lng, lat, radiusMeters, limit);
    }

    /**
     * 이름으로 찾기. 의원급처럼 비급여 가격이 없는 병원도 나온다 (찾는 병원이 "없음"으로 보이지 않게).
     * 가격 있는 병원을 먼저, 그다음 이름이 짧은(= 검색어에 더 가까운) 순. idx_hospital_name_trgm
     */
    public List<HospitalListRaw> hospitalsByName(String query, int limit) {
        return jdbc.query("""
                SELECT h.id, h.name, h.cl_cd_nm, h.addr, NULL::float8 AS dist,
                       EXISTS (SELECT 1 FROM nonpay_price_summary s WHERE s.hospital_id = h.id) AS has_prices
                  FROM hospital h
                 WHERE h.name ILIKE ? ESCAPE '\\'
                 ORDER BY has_prices DESC, length(h.name), h.name
                 LIMIT ?
                """, HOSPITAL_LIST, ItemNames.like(query), limit);
    }

    /**
     * 비급여 데이터를 마지막으로 받아온 날 = 요약 적재 잡이 마지막으로 성공한 날.
     * Spring Batch는 end_time을 JVM 시간대(로컬 PC·컨테이너 모두 Asia/Seoul) 기준 시각으로 저장 → 날짜만 자르면 KST 날짜
     */
    public Optional<java.time.LocalDate> lastNonpayCollectedOn() {
        List<java.time.LocalDate> rows = jdbc.query("""
                SELECT max(e.end_time)::date
                  FROM batch_job_execution e
                  JOIN batch_job_instance i ON i.job_instance_id = e.job_instance_id
                 WHERE i.job_name = 'nonpaySummaryJob' AND e.status = 'COMPLETED'
                """, (rs, i) -> rs.getObject(1, java.time.LocalDate.class));
        return rows.stream().filter(java.util.Objects::nonNull).findFirst();
    }

    // ── 지역 ─────────────────────────────────────────

    /** 비급여 가격이 있는 병원이 있는 시도·시군구만 */
    public List<RegionRaw> regions() {
        return jdbc.query("""
                SELECT DISTINCT h.sido_cd, h.sido_nm, h.sggu_cd, h.sggu_nm
                  FROM hospital h
                 WHERE h.id IN (SELECT DISTINCT hospital_id FROM nonpay_price_summary)
                 ORDER BY h.sido_cd, h.sggu_nm
                """, (rs, i) -> new RegionRaw(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    public Optional<RegionRaw> region(String sido, String sggu) {
        List<RegionRaw> rows = jdbc.query("""
                SELECT sido_cd, sido_nm, sggu_cd, sggu_nm FROM hospital
                 WHERE sido_cd = ? AND (?::text IS NULL OR sggu_cd = ?)
                 LIMIT 1
                """, (rs, i) -> new RegionRaw(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                sido, sggu, sggu);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public record ItemRow(String code, String korNm, String mdivNm, String sdivNm, String dtlDivNm) {
    }

    public record ItemStatRow(String code, String korNm, String mdivNm, int hospitalCount, int low, int median,
                              int high) {
    }

    /** code = 이 병원 가격의 항목 (묶음일 때 어느 제품인지) */
    public record PriceRaw(long hospitalId, String name, String kind, int minPrice, int maxPrice, Double distance,
                           String code) {
    }

    public record HospitalRow(long id, String name, String kind, String addr, String tel, String hospUrl,
                              String nonpayUrl, String sidoCd, String sidoNm, String sgguNm, Double lat, Double lng) {
    }

    public record HospitalItemRaw(String code, String name, String korNm, String mdivNm, int price,
                                  Integer median, Integer hospitalCount) {
    }

    public record HospitalListRaw(long id, String name, String kind, String addr, Double distance,
                                  boolean hasPrices) {
    }

    public record RegionRaw(String sidoCd, String sidoNm, String sgguCd, String sgguNm) {
    }
}
