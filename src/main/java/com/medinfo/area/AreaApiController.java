package com.medinfo.area;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AreaApiController {

    private final AreaRepository repository;

    public AreaApiController(AreaRepository repository) {
        this.repository = repository;
    }

    /** 기준 위치 직접 고르기용 시도·시군구 + 중심 좌표. 거의 안 바뀌어서 하루 캐시 */
    @GetMapping("/api/areas")
    public ResponseEntity<List<Area.Sido>> areas() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(group(repository.areas()));
    }

    static List<Area.Sido> group(List<AreaRepository.Row> rows) {
        Map<String, Area.Sido> bySido = new LinkedHashMap<>();
        for (AreaRepository.Row r : rows) {
            bySido.computeIfAbsent(r.sidoCd(), k -> new Area.Sido(r.sidoCd(), r.sidoNm(), new ArrayList<>()))
                    .sggu().add(new Area.Sggu(r.sgguCd(), r.sgguNm(), round(r.lat()), round(r.lng())));
        }
        return List.copyOf(bySido.values());
    }

    /** 소수 5자리(약 1m)면 충분 */
    private static double round(double v) {
        return Math.round(v * 100_000) / 100_000.0;
    }
}
