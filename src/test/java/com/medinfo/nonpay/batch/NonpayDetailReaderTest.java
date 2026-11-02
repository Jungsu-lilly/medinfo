package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayApi;
import com.medinfo.nonpay.NonpayRepository;
import com.medinfo.nonpay.NonpayRepository.DetailTarget;
import com.medinfo.publicdata.ApiOperation;
import com.medinfo.publicdata.PublicDataResponse;
import com.medinfo.publicdata.QuotaExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NonpayDetailReaderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    /** 병원 id 10, 20, 30. 20번은 상세 1,500건이라 2페이지 */
    private final List<DetailTarget> all = List.of(
            new DetailTarget(10, "Y10"), new DetailTarget(20, "Y20"), new DetailTarget(30, "Y30"));

    @Test
    void 병원마다_상세를_전부_받아_한_건으로_넘긴다() {
        FakeApi api = new FakeApi(Integer.MAX_VALUE);
        NonpayDetailReader reader = new NonpayDetailReader(api, repository(), CLOCK);
        reader.open(new ExecutionContext());

        HospitalDetail h10 = reader.read();
        HospitalDetail h20 = reader.read();
        HospitalDetail h30 = reader.read();

        assertThat(h10.rows()).hasSize(3);
        assertThat(h20.rows()).hasSize(1500);
        assertThat(h30.hospitalId()).isEqualTo(30);
        assertThat(reader.read()).isNull();
        assertThat(api.calls).containsExactly("Y10#1", "Y20#1", "Y20#2", "Y30#1");
    }

    @Test
    void 한도에_걸리면_마지막_커밋_병원_다음부터_이어받는다() {
        FakeApi first = new FakeApi(2); // 세 번째 호출(Y20 2페이지)에서 한도 초과
        NonpayDetailReader reader = new NonpayDetailReader(first, repository(), CLOCK);
        ExecutionContext context = new ExecutionContext();
        reader.open(context);

        reader.read();          // 10
        reader.update(context); // 커밋
        assertThatThrownBy(reader::read).isInstanceOf(QuotaExceededException.class); // 20 도중 실패 → 롤백

        FakeApi nextDay = new FakeApi(Integer.MAX_VALUE);
        NonpayDetailReader restarted = new NonpayDetailReader(nextDay, repository(), CLOCK);
        restarted.open(context);

        assertThat(restarted.read().hospitalId()).isEqualTo(20); // 20은 처음부터 다시
        assertThat(restarted.read().hospitalId()).isEqualTo(30);
        assertThat(restarted.read()).isNull();
        assertThat(nextDay.calls).doesNotContain("Y10#1");
    }

    /** DB 대신: id > after 인 병원을 limit개 */
    private NonpayRepository repository() {
        NonpayRepository repo = mock(NonpayRepository.class);
        when(repo.nextDetailTargets(anyLong(), any(LocalDate.class), anyInt())).thenAnswer(inv -> {
            long after = inv.getArgument(0);
            int limit = inv.getArgument(2);
            return new ArrayList<>(all.stream().filter(t -> t.hospitalId() > after).limit(limit).toList());
        });
        return repo;
    }

    static class FakeApi implements NonpayApi {
        final int okCalls;
        final List<String> calls = new ArrayList<>();

        FakeApi(int okCalls) {
            this.okCalls = okCalls;
        }

        @Override
        public PublicDataResponse summaryPage(int page) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PublicDataResponse detailPage(String ykiho, int page) {
            if (calls.size() >= okCalls) {
                throw new QuotaExceededException(ApiOperation.NONPAY_DETAIL, 9501, 10000);
            }
            calls.add(ykiho + "#" + page);
            int total = "Y20".equals(ykiho) ? 1500 : 3;
            int from = (page - 1) * PAGE_SIZE;
            int to = Math.min(total, from + PAGE_SIZE);
            List<Map<String, String>> items = IntStream.range(from, Math.max(from, to)).mapToObj(n -> {
                Map<String, String> m = new HashMap<>();
                m.put("sno", String.valueOf(n + 1));
                m.put("npayCd", "N" + n);
                m.put("yadmNpayCdNm", "항목" + n);
                m.put("adtFrDd", "20260903");
                m.put("adtEndDd", "99991231");
                m.put("curAmt", "10000");
                return m;
            }).toList();
            return new PublicDataResponse(items, total, page, PAGE_SIZE);
        }
    }
}
