package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayApi;
import com.medinfo.nonpay.NonpayMapper;
import com.medinfo.nonpay.NonpaySummaryRow;
import com.medinfo.publicdata.PublicDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import java.util.List;
import java.util.Optional;

/**
 * 전국 비급여 요약을 1000건씩 페이지 순서대로 읽는다.
 *
 * 이어받기: 청크가 커밋될 때마다 "다음에 읽을 페이지·위치"를 ExecutionContext에 저장한다 (update).
 * 한도 초과 등으로 실패하면 그 청크만 롤백되고, 같은 파라미터로 다시 실행하면 저장된 위치부터 다시 받는다.
 * → 이미 받은 페이지를 다시 호출하지 않아 한도를 아낀다.
 *
 * 스텝이 실행될 때마다 open()에서 상태를 전부 다시 잡으므로 싱글턴 빈으로 써도 된다 (스텝은 동시에 하나만 돈다).
 */
public class NonpaySummaryReader implements ItemStreamReader<NonpaySummaryRow> {

    private static final Logger log = LoggerFactory.getLogger(NonpaySummaryReader.class);
    static final String PAGE_KEY = "summary.page";
    static final String INDEX_KEY = "summary.index";
    private static final int MAX_PAGES = 1_000; // 100만 건. totalCount가 이상해도 무한 호출 방지

    private final NonpayApi api;

    private int page;
    private int index;
    private List<NonpaySummaryRow> buffer;
    private boolean lastPage;
    private int skipped;

    public NonpaySummaryReader(NonpayApi api) {
        this.api = api;
    }

    @Override
    public void open(ExecutionContext context) {
        page = context.getInt(PAGE_KEY, 1);
        index = context.getInt(INDEX_KEY, 0);
        buffer = null;
        lastPage = false;
        skipped = 0;
        if (page > 1 || index > 0) {
            log.info("비급여 요약 이어받기: {}페이지 {}번째부터", page, index);
        }
    }

    @Override
    public NonpaySummaryRow read() {
        while (true) {
            if (buffer == null) {
                fetch();
            }
            if (index < buffer.size()) {
                return buffer.get(index++);
            }
            if (lastPage) {
                log.info("비급여 요약 읽기 끝: {}페이지, 형식 오류로 건너뜀 {}건", page, skipped);
                return null;
            }
            page++;
            index = 0;
            buffer = null;
        }
    }

    /** 커밋 직전에 호출됨. 현재 페이지를 다 읽었으면 "다음 페이지 처음"으로 저장해 재시작 때 헛호출을 줄인다 */
    @Override
    public void update(ExecutionContext context) {
        boolean pageDone = buffer != null && index >= buffer.size() && !lastPage;
        context.putInt(PAGE_KEY, pageDone ? page + 1 : page);
        context.putInt(INDEX_KEY, pageDone ? 0 : index);
    }

    private void fetch() {
        PublicDataResponse response = api.summaryPage(page);
        buffer = response.items().stream()
                .map(NonpayMapper::summary)
                .flatMap(Optional::stream)
                .toList();
        skipped += response.items().size() - buffer.size();
        lastPage = response.items().isEmpty()
                || (long) page * NonpayApi.PAGE_SIZE >= response.totalCount()
                || page >= MAX_PAGES;
        if (page % 20 == 0 || page == 1) {
            log.info("비급여 요약 {}페이지 / 전체 {}건", page, response.totalCount());
        }
    }
}
