package com.medinfo.er;

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
 * 응급의료기관 목록(전국 약 530곳) 동기화. 하루 1회, 1회 호출.
 * 주소·좌표·분류·전화를 채우고 바로 매칭을 돌린다.
 * 실시간 병상(약 415곳)보다 기관이 많다: 병상 정보 없는 응급실도 목록·전화번호는 보여주기 위해 전부 저장.
 */
@Service
public class ErHospitalSyncService {

    private static final Logger log = LoggerFactory.getLogger(ErHospitalSyncService.class);

    private final PublicDataClient client;
    private final ErHospitalRepository repository;
    private final ErHospitalMatcher matcher;
    private final CollectorMetrics metrics;

    public ErHospitalSyncService(PublicDataClient client, ErHospitalRepository repository,
                                 ErHospitalMatcher matcher, CollectorMetrics metrics) {
        this.client = client;
        this.repository = repository;
        this.matcher = matcher;
        this.metrics = metrics;
    }

    public int sync() {
        PublicDataResponse response = client.call(ApiOperation.ER_LIST,
                Map.of("pageNo", "1", "numOfRows", "1000"));
        if (!response.isComplete()) {
            log.warn("응급기관 목록 불완전: totalCount={}, 수신={}", response.totalCount(), response.items().size());
        }

        List<ErHospitalRow> rows = response.items().stream()
                .map(ErHospitalMapper::map)
                .flatMap(Optional::stream)
                .toList();
        repository.upsertAll(rows);

        long noLocation = rows.stream().filter(r -> r.lon() == null).count();
        log.info("응급기관 목록 동기화 완료: 저장 {}곳, 좌표 없음 {}곳", rows.size(), noLocation);
        metrics.success("erHospitalSync", rows.size());

        matcher.match();
        return rows.size();
    }
}
