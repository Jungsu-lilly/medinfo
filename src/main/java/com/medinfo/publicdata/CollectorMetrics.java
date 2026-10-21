package com.medinfo.publicdata;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 수집 잡 공통 지표.
 *  - collector_last_success_epoch_seconds{job} : 마지막 성공 시각 → "주기 대비 너무 오래 성공 못 함" 알람
 *  - collector_last_rows{job}                  : 마지막 실행에서 저장한 건수 → 갑자기 0이나 반토막이면 이상
 */
@Component
public class CollectorMetrics {

    private final MeterRegistry registry;
    private final Clock clock;
    private final Map<String, AtomicLong> lastSuccess = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastRows = new ConcurrentHashMap<>();

    public CollectorMetrics(MeterRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
    }

    public void success(String job, long rows) {
        gauge(lastSuccess, "collector.last.success.epoch.seconds", job).set(clock.instant().getEpochSecond());
        gauge(lastRows, "collector.last.rows", job).set(rows);
    }

    private AtomicLong gauge(Map<String, AtomicLong> holder, String name, String job) {
        return holder.computeIfAbsent(job, j -> {
            AtomicLong value = new AtomicLong();
            Gauge.builder(name, value, AtomicLong::get).tag("job", j).register(registry);
            return value;
        });
    }
}
