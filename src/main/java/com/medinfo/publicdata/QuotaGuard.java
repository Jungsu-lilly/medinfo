package com.medinfo.publicdata;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 일일 호출 한도 가드 (장애 시나리오 3).
 * 호출 직전에 카운트를 올리고, 한도 × quotaBlockRatio 를 넘으면 호출하지 않고 막는다.
 * 공공데이터 한도는 자정(KST)에 초기화되므로 날짜는 Asia/Seoul 기준.
 */
@Component
public class QuotaGuard {

    private static final Logger log = LoggerFactory.getLogger(QuotaGuard.class);

    private final ApiUsageRepository repository;
    private final PublicDataProperties properties;
    private final Clock clock;
    private final Map<ApiOperation, AtomicInteger> todayCounts = new EnumMap<>(ApiOperation.class);

    public QuotaGuard(ApiUsageRepository repository, PublicDataProperties properties,
                      Clock clock, MeterRegistry registry) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
        for (ApiOperation op : ApiOperation.values()) {
            AtomicInteger counter = new AtomicInteger();
            todayCounts.put(op, counter);
            // 대시보드: 오늘 사용률(%) = public_api_quota_used / public_api_quota_limit
            Gauge.builder("public.api.quota.used", counter, AtomicInteger::get)
                    .tag("api", op.name()).register(registry);
            Gauge.builder("public.api.quota.limit", op, ApiOperation::dailyLimit)
                    .tag("api", op.name()).register(registry);
        }
    }

    public void acquire(ApiOperation op) {
        int used = repository.incrementAndGet(op, LocalDate.now(clock));
        todayCounts.get(op).set(used);

        int blockAt = (int) (op.dailyLimit() * properties.quotaBlockRatio());
        if (used > blockAt) {
            throw new QuotaExceededException(op, used, op.dailyLimit());
        }
        if (used == (int) (op.dailyLimit() * 0.9)) {
            log.warn("공공데이터 한도 90% 도달: {} {}/{}", op, used, op.dailyLimit());
        }
    }
}
