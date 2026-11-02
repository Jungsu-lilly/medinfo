package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayApi;
import com.medinfo.nonpay.NonpayRepository;
import com.medinfo.nonpay.NonpaySummaryRow;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

/**
 * 비급여 대량 적재 잡 2개. 실행·재시작 판단은 {@link NonpayLoadLauncher}가 한다.
 *
 * Spring Batch를 쓰는 이유: "하루 한도 안에 못 끝나면 멈췄다가 다음 날 이어서"를 직접 만들지 않고
 * 청크 커밋 + ExecutionContext(체크포인트) + 같은 파라미터 재실행 = 재시작으로 해결.
 * 2분 주기 응급실 수집은 재시작이 필요 없어 Batch를 쓰지 않는다 (실행 이력만 하루 720개 쌓임).
 */
@Configuration
public class NonpayBatchConfig {

    public static final String SUMMARY_JOB = "nonpaySummaryJob";
    public static final String DETAIL_JOB = "nonpayDetailJob";

    /** 요약: 1페이지(1000건) = 1청크. 페이지마다 커밋 → 실패해도 앞 페이지는 남고 거기서부터 재시작 */
    private static final int SUMMARY_CHUNK = NonpayApi.PAGE_SIZE;
    /**
     * 상세: 병원 5곳 = 1청크. 청크 트랜잭션 동안 DB 커넥션 1개를 잡은 채 API를 호출하므로(5곳 ≈ 5~10회 호출)
     * 너무 크게 잡지 않는다. 커넥션 풀이 작은데 이런 잡이 여러 개면 풀 고갈 위험 (장애 시나리오 1과 연결).
     */
    private static final int DETAIL_CHUNK = 5;

    @Bean
    public Job nonpaySummaryJob(JobRepository jobRepository, Step nonpaySummaryStep) {
        return new JobBuilder(SUMMARY_JOB, jobRepository).start(nonpaySummaryStep).build();
    }

    @Bean
    public Step nonpaySummaryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                  NonpayApi api, NonpayRepository repository) {
        return new StepBuilder("nonpaySummaryStep", jobRepository)
                .<NonpaySummaryRow, NonpaySummaryRow>chunk(SUMMARY_CHUNK)
                .reader(new NonpaySummaryReader(api))
                .writer(new NonpaySummaryWriter(repository))
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public Job nonpayDetailJob(JobRepository jobRepository, Step nonpayDetailStep) {
        return new JobBuilder(DETAIL_JOB, jobRepository).start(nonpayDetailStep).build();
    }

    @Bean
    public Step nonpayDetailStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                 NonpayApi api, NonpayRepository repository, Clock clock) {
        return new StepBuilder("nonpayDetailStep", jobRepository)
                .<HospitalDetail, HospitalDetail>chunk(DETAIL_CHUNK)
                .reader(new NonpayDetailReader(api, repository, clock))
                .writer(new NonpayDetailWriter(repository))
                .transactionManager(transactionManager)
                .build();
    }
}
