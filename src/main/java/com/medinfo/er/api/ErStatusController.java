package com.medinfo.er.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 응급실 실시간 현황 (기능 3).
 * 외부 API를 요청마다 부르지 않고 2분마다 수집해 둔 스냅샷만 읽는다 → 외부 API가 느려도 이 API는 영향 없음
 * (장애 시나리오 2의 "개선 후" 구조).
 */
@RestController
public class ErStatusController {

    /** 기본 위치: 서울 마포구청 (위치 권한이 없을 때) */
    static final double DEFAULT_LAT = 37.5663;
    static final double DEFAULT_LNG = 126.9019;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ErStatusRepository repository;
    private final Clock clock;
    private final int staleMinutes;

    public ErStatusController(ErStatusRepository repository, Clock clock,
                              @Value("${er.stale-minutes:30}") int staleMinutes) {
        this.repository = repository;
        this.clock = clock;
        this.staleMinutes = staleMinutes;
    }

    @GetMapping("/api/er")
    public ResponseEntity<ErStatus> near(
            @RequestParam(defaultValue = "" + DEFAULT_LAT) double lat,
            @RequestParam(defaultValue = "" + DEFAULT_LNG) double lng,
            @RequestParam(defaultValue = "20000") int radius,
            @RequestParam(defaultValue = "30") int limit) {

        // 한국 밖 좌표·과한 반경은 기본값으로 (잘못된 요청이 무거운 쿼리가 되지 않게)
        if (lat < 33 || lat > 39.5 || lng < 124 || lng > 132) {
            lat = DEFAULT_LAT;
            lng = DEFAULT_LNG;
        }
        int safeRadius = Math.clamp(radius, 1_000, 50_000);
        int safeLimit = Math.clamp(limit, 1, 100);

        OffsetDateTime now = OffsetDateTime.now(clock);
        List<ErStatus.Item> items = repository.findNear(lat, lng, safeRadius, safeLimit).stream()
                .map(row -> toItem(row, now))
                .toList();
        OffsetDateTime last = repository.lastCollectedAt();

        ErStatus body = new ErStatus(
                format(now.truncatedTo(ChronoUnit.SECONDS)),
                last == null ? null : format(last),
                items);

        // 수집 주기가 2분이라 30초 캐시는 신선도에 영향이 거의 없고, 몰릴 때 DB를 보호한다
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(body);
    }

    private ErStatus.Item toItem(ErStatusRepository.Row row, OffsetDateTime now) {
        Integer minutes = row.hvidate() == null ? null
                : (int) Math.max(0, Duration.between(row.hvidate(), now).toMinutes());
        boolean stale = minutes != null && minutes >= staleMinutes;
        return new ErStatus.Item(
                row.hpid(),
                DisplayNames.clean(row.name()),
                row.kind(),
                row.tel(),
                row.addr(),
                row.lat(),
                row.lng(),
                (int) Math.round(row.distanceMeters()),
                row.hospitalId(),
                row.hvec(),
                row.hvs01(),
                row.hvidate() == null ? null : format(row.hvidate()),
                minutes,
                stale);
    }

    private static String format(OffsetDateTime t) {
        return t.atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).toOffsetDateTime().format(ISO);
    }
}
