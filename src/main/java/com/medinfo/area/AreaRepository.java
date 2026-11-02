package com.medinfo.area;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AreaRepository {

    private final JdbcTemplate jdbc;

    public AreaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 시군구별 중심 좌표 = 그 시군구 요양기관(의원 포함) 좌표의 중앙값.
     * 행정구역 경계 데이터 없이도 "그 구에서 사람이 많은 쪽"에 가까운 점이 나온다.
     * 평균 대신 중앙값: 주소가 잘못 변환된 좌표 몇 개에 끌려가지 않게
     */
    public List<Row> areas() {
        return jdbc.query("""
                SELECT sido_cd, sido_nm, sggu_cd, sggu_nm,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY ST_Y(location::geometry)) AS lat,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY ST_X(location::geometry)) AS lng
                  FROM hospital
                 WHERE location IS NOT NULL
                 GROUP BY sido_cd, sido_nm, sggu_cd, sggu_nm
                 ORDER BY sido_cd, sggu_nm
                """, (rs, i) -> new Row(rs.getString("sido_cd"), rs.getString("sido_nm"),
                rs.getString("sggu_cd"), rs.getString("sggu_nm"), rs.getDouble("lat"), rs.getDouble("lng")));
    }

    public record Row(String sidoCd, String sidoNm, String sgguCd, String sgguNm, double lat, double lng) {
    }
}
