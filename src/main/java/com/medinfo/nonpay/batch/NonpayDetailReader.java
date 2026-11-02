package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayApi;
import com.medinfo.nonpay.NonpayDetailRow;
import com.medinfo.nonpay.NonpayMapper;
import com.medinfo.nonpay.NonpayRepository;
import com.medinfo.nonpay.NonpayRepository.DetailTarget;
import com.medinfo.publicdata.PublicDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * 요약 가격이 있는 병원을 id 순서로 하나씩 꺼내 상세를 전부 받아온다 (병원 수천 곳 → 호출 수천 회).
 *
 * 이어받기: 커밋된 마지막 병원 id를 저장한다. 하루 한도(10,000)를 넘기면 그 청크만 롤백되고
 * 다음 실행(다음 날)에 그 다음 병원부터 이어서 받는다.
 */
public class NonpayDetailReader implements ItemStreamReader<HospitalDetail> {

    private static final Logger log = LoggerFactory.getLogger(NonpayDetailReader.class);
    static final String LAST_ID_KEY = "detail.lastHospitalId";
    private static final int TARGET_BATCH = 200;
    private static final int MAX_PAGES_PER_HOSPITAL = 20; // 2만 건. 상급종합도 1,300건 수준

    private final NonpayApi api;
    private final NonpayRepository repository;
    private final Clock clock;

    private long lastHospitalId;   // 마지막으로 넘겨준 병원 (체크포인트)
    private long loadedUpTo;       // 대상 목록을 어디까지 DB에서 가져왔는지
    private final Deque<DetailTarget> targets = new ArrayDeque<>();
    private boolean noMoreTargets;
    private int hospitals;

    public NonpayDetailReader(NonpayApi api, NonpayRepository repository, Clock clock) {
        this.api = api;
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void open(ExecutionContext context) {
        lastHospitalId = context.getLong(LAST_ID_KEY, 0L);
        loadedUpTo = lastHospitalId;
        targets.clear();
        noMoreTargets = false;
        hospitals = 0;
        if (lastHospitalId > 0) {
            log.info("비급여 상세 이어받기: 병원 id {} 다음부터", lastHospitalId);
        }
    }

    @Override
    public HospitalDetail read() {
        if (targets.isEmpty() && !noMoreTargets) {
            List<DetailTarget> next = repository.nextDetailTargets(loadedUpTo, LocalDate.now(clock), TARGET_BATCH);
            if (next.isEmpty()) {
                noMoreTargets = true;
            } else {
                targets.addAll(next);
                loadedUpTo = next.get(next.size() - 1).hospitalId();
            }
        }
        DetailTarget target = targets.poll();
        if (target == null) {
            log.info("비급여 상세 읽기 끝: 이번 실행 {}곳", hospitals);
            return null;
        }
        List<NonpayDetailRow> rows = fetchAll(target.ykiho());
        lastHospitalId = target.hospitalId();
        hospitals++;
        if (hospitals % 200 == 0) {
            log.info("비급여 상세 진행: 이번 실행 {}곳 (병원 id {})", hospitals, lastHospitalId);
        }
        return new HospitalDetail(target.hospitalId(), rows);
    }

    @Override
    public void update(ExecutionContext context) {
        context.putLong(LAST_ID_KEY, lastHospitalId);
    }

    private List<NonpayDetailRow> fetchAll(String ykiho) {
        List<NonpayDetailRow> rows = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES_PER_HOSPITAL; page++) {
            PublicDataResponse response = api.detailPage(ykiho, page);
            response.items().stream().map(NonpayMapper::detail).flatMap(Optional::stream).forEach(rows::add);
            if (response.items().isEmpty() || (long) page * NonpayApi.PAGE_SIZE >= response.totalCount()) {
                break;
            }
        }
        return rows;
    }
}
