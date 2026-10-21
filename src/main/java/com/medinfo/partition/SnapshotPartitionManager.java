package com.medinfo.partition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * er_bed_snapshot 일 단위 파티션 관리.
 *  - 오늘·내일 파티션이 없으면 만든다 (자정 직후 수집이 실패하지 않게 하루 앞서)
 *  - 보관 기간이 지난 파티션은 DROP (DELETE보다 훨씬 가볍다: 하루 약 30만 행을 통째로 제거)
 * 기동 시 1회 + 매시간 실행. 몇 번을 실행해도 결과가 같다(멱등).
 */
@Component
public class SnapshotPartitionManager {

    private static final Logger log = LoggerFactory.getLogger(SnapshotPartitionManager.class);
    private static final DateTimeFormatter SUFFIX = DateTimeFormatter.BASIC_ISO_DATE; // 20261020

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final int retentionDays;

    public SnapshotPartitionManager(JdbcTemplate jdbcTemplate, Clock clock,
                                    @Value("${collector.snapshot-retention-days}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    public void maintain() {
        LocalDate today = LocalDate.now(clock);
        createIfAbsent(today);
        createIfAbsent(today.plusDays(1));
        dropExpired(today);
    }

    private void createIfAbsent(LocalDate day) {
        // 날짜는 내부에서 만든 값이라 SQL 조립해도 주입 위험 없음
        String sql = """
                CREATE TABLE IF NOT EXISTS er_bed_snapshot_%s PARTITION OF er_bed_snapshot
                FOR VALUES FROM ('%s 00:00:00+09') TO ('%s 00:00:00+09')
                """.formatted(day.format(SUFFIX), day, day.plusDays(1));
        jdbcTemplate.execute(sql);
    }

    private void dropExpired(LocalDate today) {
        // 보관 기간 직후부터 2주치를 확인 (서버가 며칠 꺼져 있었어도 정리되도록)
        for (int i = retentionDays + 1; i <= retentionDays + 14; i++) {
            String table = "er_bed_snapshot_" + today.minusDays(i).format(SUFFIX);
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_class WHERE relname = ?", Integer.class, table);
            if (exists != null && exists > 0) {
                jdbcTemplate.execute("DROP TABLE " + table);
                log.info("만료 파티션 삭제: {}", table);
            }
        }
    }
}
