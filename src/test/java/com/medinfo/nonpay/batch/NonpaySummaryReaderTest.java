package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayApi;
import com.medinfo.nonpay.NonpaySummaryRow;
import com.medinfo.publicdata.PublicDataResponse;
import com.medinfo.publicdata.QuotaExceededException;
import com.medinfo.publicdata.ApiOperation;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonpaySummaryReaderTest {

    private static final int TOTAL = 2_500; // 1000 + 1000 + 500 → 3페이지

    @Test
    void 전체_페이지를_순서대로_읽는다() {
        FakeApi api = new FakeApi(TOTAL, Integer.MAX_VALUE);
        NonpaySummaryReader reader = new NonpaySummaryReader(api);
        reader.open(new ExecutionContext());

        List<NonpaySummaryRow> rows = readAll(reader);

        assertThat(rows).hasSize(TOTAL);
        assertThat(rows.get(0).npayCd()).isEqualTo("N0");
        assertThat(rows.get(TOTAL - 1).npayCd()).isEqualTo("N" + (TOTAL - 1));
        assertThat(api.calledPages).containsExactly(1, 2, 3);
    }

    @Test
    void 한도에_걸려_실패하면_커밋된_위치부터_이어받는다() {
        // 2페이지 호출에서 한도 초과
        FakeApi first = new FakeApi(TOTAL, 1);
        NonpaySummaryReader reader = new NonpaySummaryReader(first);
        ExecutionContext context = new ExecutionContext();
        reader.open(context);

        for (int i = 0; i < 1000; i++) {
            reader.read();
        }
        reader.update(context); // 1청크(1페이지) 커밋
        assertThatThrownBy(reader::read).isInstanceOf(QuotaExceededException.class);

        // 다음 날: 같은 ExecutionContext로 다시 열면 2페이지부터
        FakeApi nextDay = new FakeApi(TOTAL, Integer.MAX_VALUE);
        NonpaySummaryReader restarted = new NonpaySummaryReader(nextDay);
        restarted.open(context);
        List<NonpaySummaryRow> rest = readAll(restarted);

        assertThat(rest).hasSize(TOTAL - 1000);
        assertThat(rest.get(0).npayCd()).isEqualTo("N1000");
        assertThat(nextDay.calledPages).containsExactly(2, 3); // 1페이지는 다시 부르지 않음
    }

    @Test
    void 페이지_중간에서_커밋됐으면_그_위치부터() {
        FakeApi api = new FakeApi(TOTAL, Integer.MAX_VALUE);
        NonpaySummaryReader reader = new NonpaySummaryReader(api);
        ExecutionContext context = new ExecutionContext();
        reader.open(context);
        for (int i = 0; i < 1300; i++) {
            reader.read();
        }
        reader.update(context);

        NonpaySummaryReader restarted = new NonpaySummaryReader(new FakeApi(TOTAL, Integer.MAX_VALUE));
        restarted.open(context);

        assertThat(restarted.read().npayCd()).isEqualTo("N1300");
    }

    @Test
    void 형식이_틀린_건은_건너뛰고_계속_읽는다() {
        FakeApi api = new FakeApi(10, Integer.MAX_VALUE);
        api.broken = 3; // 3번째 건 가격 누락
        NonpaySummaryReader reader = new NonpaySummaryReader(api);
        reader.open(new ExecutionContext());

        assertThat(readAll(reader)).hasSize(9);
    }

    private static List<NonpaySummaryRow> readAll(NonpaySummaryReader reader) {
        List<NonpaySummaryRow> rows = new ArrayList<>();
        NonpaySummaryRow row;
        while ((row = reader.read()) != null) {
            rows.add(row);
        }
        return rows;
    }

    /** total건을 1000건씩 돌려주는 가짜 API. okUntilPage 다음 페이지부터 한도 초과 */
    static class FakeApi implements NonpayApi {
        final int total;
        final int okUntilPage;
        final List<Integer> calledPages = new ArrayList<>();
        int broken = -1;

        FakeApi(int total, int okUntilPage) {
            this.total = total;
            this.okUntilPage = okUntilPage;
        }

        @Override
        public PublicDataResponse summaryPage(int page) {
            if (page > okUntilPage) {
                throw new QuotaExceededException(ApiOperation.NONPAY_SUMMARY, 9501, 10000);
            }
            calledPages.add(page);
            int from = (page - 1) * PAGE_SIZE;
            int to = Math.min(total, from + PAGE_SIZE);
            List<Map<String, String>> items = IntStream.range(from, Math.max(from, to))
                    .mapToObj(this::item).toList();
            return new PublicDataResponse(items, total, page, PAGE_SIZE);
        }

        @Override
        public PublicDataResponse detailPage(String ykiho, int page) {
            throw new UnsupportedOperationException();
        }

        private Map<String, String> item(int n) {
            Map<String, String> m = new HashMap<>();
            m.put("ykiho", "Y" + (n % 7));
            m.put("yadmNm", "병원" + (n % 7));
            m.put("clCd", "11");
            m.put("clCdNm", "종합병원");
            m.put("sidoCd", "110000");
            m.put("sidoCdNm", "서울");
            m.put("sgguCd", "110016");
            m.put("sgguCdNm", "종로구");
            m.put("npayCd", "N" + n);
            m.put("npayKorNm", "항목" + n);
            m.put("adtFrDd", "20260903");
            m.put("adtEndDd", "99991231");
            m.put("minPrc", n == broken - 1 ? "" : "10000");
            m.put("maxPrc", "20000");
            return m;
        }
    }
}
