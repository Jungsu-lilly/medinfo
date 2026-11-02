package com.medinfo.nonpay.api;

import com.medinfo.nonpay.api.ApiModels.HospitalListItem;
import com.medinfo.nonpay.api.ApiModels.HospitalPage;
import com.medinfo.nonpay.api.ApiModels.ItemSummary;
import com.medinfo.nonpay.api.ApiModels.PriceList;
import com.medinfo.nonpay.api.ApiModels.Region;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 가격 비교(기능 1)·병원 상세(기능 2) 조회 API.
 * 데이터는 월 1회 적재라 자주 안 바뀐다 → 응답 캐시를 길게(10분~1일) 둬서 몰릴 때 DB를 보호.
 */
@RestController
public class PriceApiController {

    static final String DEFAULT_SIDO = "110000"; // 서울
    static final double DEFAULT_LAT = 37.5663;   // 마포구청 (응급실 API와 같은 기본 위치)
    static final double DEFAULT_LNG = 126.9019;

    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9]{1,50}");
    private static final Pattern REGION_CODE = Pattern.compile("\\d{6}");
    private static final CacheControl TEN_MINUTES = CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic();

    private final PriceService service;

    public PriceApiController(PriceService service) {
        this.service = service;
    }

    /** 항목 검색: "무릎 MRI", "독감" … 병원이 많은 순 */
    @GetMapping("/api/items")
    public ResponseEntity<List<ItemSummary>> search(@RequestParam(defaultValue = "") String q,
                                                    @RequestParam(defaultValue = "30") int limit) {
        String query = q.trim();
        if (query.isEmpty() || query.length() > 50) {
            return ResponseEntity.ok().cacheControl(TEN_MINUTES).body(List.of());
        }
        return ResponseEntity.ok().cacheControl(TEN_MINUTES).body(service.search(query, Math.clamp(limit, 1, 50)));
    }

    /** 홈 "많이 찾는 비급여" */
    @GetMapping("/api/items/featured")
    public ResponseEntity<List<ItemSummary>> featured(@RequestParam(defaultValue = DEFAULT_SIDO) String sido,
                                                      @RequestParam(defaultValue = "6") int limit) {
        String safeSido = REGION_CODE.matcher(sido).matches() ? sido : DEFAULT_SIDO;
        return ResponseEntity.ok().cacheControl(TEN_MINUTES).body(service.featured(safeSido, Math.clamp(limit, 1, 20)));
    }

    /**
     * 항목 하나의 병원별 가격. 위치(lat·lng)가 오면 반경 안, 아니면 시도(+시군구) 안.
     * 가격 낮은 순 정렬, 최대 500곳.
     */
    @GetMapping("/api/prices/{code}")
    public ResponseEntity<PriceList> prices(@PathVariable String code,
                                            @RequestParam(defaultValue = DEFAULT_SIDO) String sido,
                                            @RequestParam(required = false) String sggu,
                                            @RequestParam(required = false) Double lat,
                                            @RequestParam(required = false) Double lng,
                                            @RequestParam(defaultValue = "5000") int radius) {
        if (!CODE.matcher(code).matches()) {
            return ResponseEntity.notFound().build();
        }
        var result = lat != null && lng != null && inKorea(lat, lng)
                ? service.pricesNear(code, lat, lng, Math.clamp(radius, 1_000, 30_000))
                : service.pricesInRegion(code,
                        REGION_CODE.matcher(sido).matches() ? sido : DEFAULT_SIDO,
                        sggu != null && REGION_CODE.matcher(sggu).matches() ? sggu : null);
        return result.map(body -> ResponseEntity.ok().cacheControl(TEN_MINUTES).body(body))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/hospitals/{id}")
    public ResponseEntity<HospitalPage> hospital(@PathVariable long id) {
        return service.hospital(id)
                .map(body -> ResponseEntity.ok().cacheControl(TEN_MINUTES).body(body))
                .orElse(ResponseEntity.notFound().build());
    }

    /** 병원 찾기: 이름(q)이 있으면 이름으로, 없으면 위치 기준 가까운 순 (비급여 가격이 있는 병원만) */
    @GetMapping("/api/hospitals")
    public ResponseEntity<List<HospitalListItem>> hospitals(@RequestParam(defaultValue = "") String q,
                                                            @RequestParam(defaultValue = "" + DEFAULT_LAT) double lat,
                                                            @RequestParam(defaultValue = "" + DEFAULT_LNG) double lng,
                                                            @RequestParam(defaultValue = "5000") int radius,
                                                            @RequestParam(defaultValue = "30") int limit) {
        int safeLimit = Math.clamp(limit, 1, 50);
        String query = q.trim();
        List<HospitalListItem> body;
        if (!query.isEmpty()) {
            body = query.length() > 50 ? List.of() : service.hospitalsByName(query, safeLimit);
        } else {
            boolean ok = inKorea(lat, lng);
            body = service.hospitalsNear(ok ? lat : DEFAULT_LAT, ok ? lng : DEFAULT_LNG,
                    Math.clamp(radius, 1_000, 30_000), safeLimit);
        }
        return ResponseEntity.ok().cacheControl(TEN_MINUTES).body(body);
    }

    /** 지역 선택 목록 (비급여 가격이 있는 시도·시군구만) */
    @GetMapping("/api/regions")
    public ResponseEntity<List<Region>> regions() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(service.regions());
    }

    private static boolean inKorea(double lat, double lng) {
        return lat >= 33 && lat <= 39.5 && lng >= 124 && lng <= 132;
    }
}
