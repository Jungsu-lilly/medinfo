package com.medinfo.hospital;

import com.medinfo.er.ErHospitalMatcher;
import com.medinfo.publicdata.ApiOperation;
import com.medinfo.publicdata.CollectorMetrics;
import com.medinfo.publicdata.PublicDataClient;
import com.medinfo.publicdata.PublicDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 심평원 병원정보(전국 요양기관 약 8만 곳) 동기화. 주 1회.
 * 1000건씩 약 80페이지를 순서대로 받아 페이지마다 upsert → 메모리에 전체를 올리지 않는다(장애 시나리오 4와 같은 원칙).
 * 끝나면 응급기관 매핑을 다시 돌린다 (병원 좌표·이름이 바뀌었을 수 있음).
 */
@Service
public class HospitalSyncService {

    private static final Logger log = LoggerFactory.getLogger(HospitalSyncService.class);
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 300; // 30만 곳. totalCount가 이상하게 와도 무한루프 방지

    private final PublicDataClient client;
    private final HospitalRepository repository;
    private final ErHospitalMatcher matcher;
    private final CollectorMetrics metrics;

    public HospitalSyncService(PublicDataClient client, HospitalRepository repository,
                               ErHospitalMatcher matcher, CollectorMetrics metrics) {
        this.client = client;
        this.repository = repository;
        this.matcher = matcher;
        this.metrics = metrics;
    }

    public int sync() {
        long started = System.currentTimeMillis();
        int page = 1;
        int fetched = 0;
        int saved = 0;
        int skipped = 0;
        int total = Integer.MAX_VALUE;

        while (fetched < total && page <= MAX_PAGES) {
            PublicDataResponse response = client.call(ApiOperation.HOSP_BASIS,
                    Map.of("pageNo", String.valueOf(page), "numOfRows", String.valueOf(PAGE_SIZE)));
            total = response.totalCount();
            if (response.items().isEmpty()) {
                break;
            }
            fetched += response.items().size();

            List<HospitalRow> rows = response.items().stream()
                    .map(HospitalMapper::map)
                    .flatMap(Optional::stream)
                    .toList();
            skipped += response.items().size() - rows.size();
            repository.upsertAll(rows);
            saved += rows.size();

            if (page % 10 == 0) {
                log.info("병원정보 동기화 진행: {}페이지, {}/{}건", page, fetched, total);
            }
            page++;
        }

        log.info("병원정보 동기화 완료: 저장 {}곳, 건너뜀 {}곳, 전체 {}곳, {}초",
                saved, skipped, total, (System.currentTimeMillis() - started) / 1000);
        metrics.success("hospitalSync", saved);

        matcher.match();
        return saved;
    }
}
