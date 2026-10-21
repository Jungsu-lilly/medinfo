package com.medinfo.publicdata;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class ApiUsageRepository {

    private static final String INCREMENT_SQL = """
            INSERT INTO api_usage_daily (api_name, use_date, call_count, daily_limit)
            VALUES (?, ?, 1, ?)
            ON CONFLICT (api_name, use_date)
            DO UPDATE SET call_count = api_usage_daily.call_count + 1
            RETURNING call_count
            """;

    private final JdbcTemplate jdbcTemplate;

    public ApiUsageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 오늘 호출 수를 +1 하고 증가된 값을 돌려준다. 인스턴스가 여러 대여도 DB에서 원자적으로 증가 */
    public int incrementAndGet(ApiOperation op, LocalDate date) {
        Integer count = jdbcTemplate.queryForObject(INCREMENT_SQL, Integer.class,
                op.name(), date, op.dailyLimit());
        return count == null ? 0 : count;
    }
}
