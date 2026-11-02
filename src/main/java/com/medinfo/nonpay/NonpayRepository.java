package com.medinfo.nonpay;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 비급여 적재 SQL. 트랜잭션은 걸지 않는다 → 배치 청크 트랜잭션에 그대로 참여 (청크 단위 커밋·롤백).
 */
@Repository
public class NonpayRepository {

    /**
     * 요약에 나온 병원이 아직 hospital에 없으면 요약에 있는 정보로 먼저 만든다 (좌표·주소는 병원정보 동기화가 채움).
     * 있으면 비급여 안내 URL만 바뀐 경우에 갱신. 다른 컬럼은 병원정보 동기화가 주인이라 건드리지 않는다.
     */
    private static final String UPSERT_HOSPITAL_SQL = """
            INSERT INTO hospital (ykiho, name, cl_cd, cl_cd_nm, sido_cd, sido_nm, sggu_cd, sggu_nm, nonpay_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (ykiho) DO UPDATE SET nonpay_url = EXCLUDED.nonpay_url
            WHERE hospital.nonpay_url IS DISTINCT FROM EXCLUDED.nonpay_url
            """;

    private static final String UPSERT_ITEM_SQL = """
            INSERT INTO nonpay_item (npay_cd, kor_nm, mdiv_cd, mdiv_nm, sdiv_cd, sdiv_nm, dtl_div_cd, dtl_div_nm)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (npay_cd) DO UPDATE SET
                kor_nm = EXCLUDED.kor_nm,
                mdiv_cd = EXCLUDED.mdiv_cd, mdiv_nm = EXCLUDED.mdiv_nm,
                sdiv_cd = EXCLUDED.sdiv_cd, sdiv_nm = EXCLUDED.sdiv_nm,
                dtl_div_cd = EXCLUDED.dtl_div_cd, dtl_div_nm = EXCLUDED.dtl_div_nm
            """;

    /** hospital_id는 ykiho로 찾아 넣는다 (병원은 바로 위에서 보장) */
    private static final String UPSERT_SUMMARY_SQL = """
            INSERT INTO nonpay_price_summary (hospital_id, npay_cd, adt_fr_dd, adt_end_dd, min_prc, max_prc, sido_cd, sggu_cd)
            SELECT h.id, ?, ?, ?, ?, ?, ?, ?
            FROM hospital h
            WHERE h.ykiho = ?
            ON CONFLICT (hospital_id, npay_cd, adt_fr_dd) DO UPDATE SET
                adt_end_dd = EXCLUDED.adt_end_dd,
                min_prc = EXCLUDED.min_prc,
                max_prc = EXCLUDED.max_prc,
                sido_cd = EXCLUDED.sido_cd,
                sggu_cd = EXCLUDED.sggu_cd
            """;

    private static final String INSERT_DETAIL_SQL = """
            INSERT INTO nonpay_price_detail (hospital_id, sno, adt_fr_dd, adt_end_dd, npay_cd, yadm_npay_cd_nm, cur_amt)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (hospital_id, sno, adt_fr_dd) DO UPDATE SET
                adt_end_dd = EXCLUDED.adt_end_dd,
                npay_cd = EXCLUDED.npay_cd,
                yadm_npay_cd_nm = EXCLUDED.yadm_npay_cd_nm,
                cur_amt = EXCLUDED.cur_amt
            """;

    /** 상세를 받을 병원: 현재 적용 중인 요약 가격이 있는 병원. id 순서로 이어받기 위해 id > ? */
    private static final String NEXT_DETAIL_HOSPITALS_SQL = """
            SELECT h.id, h.ykiho
            FROM hospital h
            WHERE h.id > ?
              AND EXISTS (SELECT 1 FROM nonpay_price_summary s
                          WHERE s.hospital_id = h.id AND s.adt_end_dd >= ?)
            ORDER BY h.id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public NonpayRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 요약 한 청크 저장: 병원 → 항목 → 가격 순서 (FK 때문) */
    public void saveSummary(List<? extends NonpaySummaryRow> rows) {
        // 병원은 ykiho 오름차순으로 잠근다. 병원정보 동기화도 같은 순서라 동시에 돌아도 교착(deadlock)이 안 생김
        Map<String, NonpaySummaryRow> hospitals = new LinkedHashMap<>();
        rows.stream()
                .sorted(Comparator.comparing(NonpaySummaryRow::ykiho))
                .forEach(r -> hospitals.putIfAbsent(r.ykiho(), r));
        jdbcTemplate.batchUpdate(UPSERT_HOSPITAL_SQL, hospitals.values().stream()
                .map(r -> new Object[]{r.ykiho(), r.hospitalName(), r.clCd(), r.clCdNm(),
                        r.sidoCd(), r.sidoNm(), r.sgguCd(), r.sgguNm(), r.urlAddr()})
                .toList());

        Map<String, NonpaySummaryRow> items = new LinkedHashMap<>();
        rows.stream()
                .sorted(Comparator.comparing(NonpaySummaryRow::npayCd))
                .forEach(r -> items.putIfAbsent(r.npayCd(), r));
        jdbcTemplate.batchUpdate(UPSERT_ITEM_SQL, items.values().stream()
                .map(r -> new Object[]{r.npayCd(), r.korNm(), r.mdivCd(), r.mdivNm(),
                        r.sdivCd(), r.sdivNm(), r.dtlDivCd(), r.dtlDivNm()})
                .toList());

        jdbcTemplate.batchUpdate(UPSERT_SUMMARY_SQL, rows.stream()
                .map(r -> new Object[]{r.npayCd(), Date.valueOf(r.adtFrDd()), Date.valueOf(r.adtEndDd()),
                        r.minPrc(), r.maxPrc(), r.sidoCd(), r.sgguCd(), r.ykiho()})
                .toList());
    }

    /** 병원 1곳의 상세를 통째로 교체 (API에서 빠진 항목이 DB에 남지 않게) */
    public void replaceDetail(long hospitalId, List<NonpayDetailRow> rows) {
        jdbcTemplate.update("DELETE FROM nonpay_price_detail WHERE hospital_id = ?", hospitalId);
        jdbcTemplate.batchUpdate(INSERT_DETAIL_SQL, rows.stream()
                .map(r -> new Object[]{hospitalId, r.sno(), Date.valueOf(r.adtFrDd()), Date.valueOf(r.adtEndDd()),
                        r.npayCd(), r.yadmNpayCdNm(), r.curAmt()})
                .toList());
    }

    public List<DetailTarget> nextDetailTargets(long afterHospitalId, LocalDate today, int limit) {
        return new ArrayList<>(jdbcTemplate.query(NEXT_DETAIL_HOSPITALS_SQL,
                (rs, i) -> new DetailTarget(rs.getLong("id"), rs.getString("ykiho")),
                afterHospitalId, Date.valueOf(today), limit));
    }

    public long countSummary() {
        Long n = jdbcTemplate.queryForObject("SELECT count(*) FROM nonpay_price_summary", Long.class);
        return n == null ? 0 : n;
    }

    public long countDetail() {
        Long n = jdbcTemplate.queryForObject("SELECT count(*) FROM nonpay_price_detail", Long.class);
        return n == null ? 0 : n;
    }

    /** 상세를 받을 병원 1곳 */
    public record DetailTarget(long hospitalId, String ykiho) {
    }
}
