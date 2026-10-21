package com.medinfo.er;

import com.medinfo.publicdata.ApiOperation;
import com.medinfo.publicdata.PublicDataClient;
import com.medinfo.publicdata.PublicDataResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 응급실 실시간 가용병상 수집 (2분 주기, 1회 호출로 전국 415곳).
 *
 * Spring Batch가 아니라 단순 서비스로 둔 이유: 2분마다 돌면 하루 720개의 잡 실행 이력이 메타 테이블에 쌓이는데,
 * 재시작·청크 같은 Batch 기능이 필요 없는 작업이라 득보다 실이 크다. 대량 적재 잡만 Spring Batch로.
 *
 * 지표
 *  - collector_last_success_epoch_seconds{job="erBed"} : 마지막 성공 시각 → "10분 넘게 수집 안 됨" 알람
 *  - er_data_staleness_seconds : 수집 시각 − 병원 입력 시각(hvidate)의 중앙값 → 데이터 신선도
 *  - er_collected_hospitals : 이번에 저장한 기관 수
 */
@Service
public class ErBedCollector {

    private static final Logger log = LoggerFactory.getLogger(ErBedCollector.class);

    private final PublicDataClient client;
    private final ErBedRepository repository;
    private final Clock clock;

    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong();
    private final AtomicLong stalenessSeconds = new AtomicLong();
    private final AtomicLong collectedCount = new AtomicLong();

    public ErBedCollector(PublicDataClient client, ErBedRepository repository, Clock clock, MeterRegistry registry) {
        this.client = client;
        this.repository = repository;
        this.clock = clock;
        Gauge.builder("collector.last.success.epoch.seconds", lastSuccessEpochSeconds, AtomicLong::get)
                .tag("job", "erBed").register(registry);
        Gauge.builder("er.data.staleness.seconds", stalenessSeconds, AtomicLong::get).register(registry);
        Gauge.builder("er.collected.hospitals", collectedCount, AtomicLong::get).register(registry);
    }

    public int collect() {
        OffsetDateTime collectedAt = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

        PublicDataResponse response = client.call(ApiOperation.ER_REALTIME_BEDS,
                Map.of("pageNo", "1", "numOfRows", "1000"));

        if (!response.isComplete()) {
            // 기관 수가 1000을 넘으면 페이지 처리가 필요해진다는 신호
            log.warn("응급실 전국 수집 불완전: totalCount={}, 수신={}", response.totalCount(), response.items().size());
        }

        List<ErBedRow> rows = new ArrayList<>();
        int skipped = 0;
        for (Map<String, String> item : response.items()) {
            Optional<ErBedRow> row = ErBedMapper.map(item);
            if (row.isPresent()) {
                rows.add(row.get());
            } else {
                skipped++;
            }
        }

        repository.saveSnapshot(rows, collectedAt);

        lastSuccessEpochSeconds.set(collectedAt.toEpochSecond());
        stalenessSeconds.set(medianStalenessSeconds(rows, collectedAt));
        collectedCount.set(rows.size());
        log.info("응급실 수집 완료: 저장 {}곳, 건너뜀 {}곳, 신선도 중앙값 {}초", rows.size(), skipped, stalenessSeconds.get());
        return rows.size();
    }

    static long medianStalenessSeconds(List<ErBedRow> rows, OffsetDateTime collectedAt) {
        if (rows.isEmpty()) {
            return 0;
        }
        long[] lags = rows.stream()
                .mapToLong(r -> Math.max(0, Duration.between(r.hvidate(), collectedAt).getSeconds()))
                .sorted()
                .toArray();
        return lags[lags.length / 2];
    }
}
