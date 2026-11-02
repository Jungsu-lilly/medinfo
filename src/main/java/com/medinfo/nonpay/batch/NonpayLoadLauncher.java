package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayRepository;
import com.medinfo.publicdata.CollectorMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;

/**
 * 비급여 적재 실행기. Quartz가 매시간 부르고, 할 일이 있을 때만 잡을 돌린다.
 *
 *  - 회차(runKey) = 이번 달 "2026-11". 한 달에 한 번 전체를 다시 받는다
 *    (데이터는 연 1회 9월 공개지만 병원 정정분 반영 + 한도 대비 부담 없음: 요약 약 195회 + 상세 약 5천 회)
 *  - 순서: 요약 완료 → 상세. 요약이 이번 회차에 완료돼 있지 않으면 상세는 시작하지 않는다
 *  - 이미 완료 → 아무것도 안 함 (API 호출 0)
 *  - 실패(한도 초과·네트워크) → 같은 파라미터로 다시 실행 = Spring Batch 재시작 → 체크포인트부터 이어받기
 *  - STARTED로 남아 있음 → 서버가 실행 중에 죽은 것. Quartz 클러스터가 이 잡의 동시 실행을 막으므로
 *    지금 다른 곳에서 돌고 있을 수 없다 → FAILED로 바꾸고(recover) 재시작
 */
@Component
public class NonpayLoadLauncher {

    private static final Logger log = LoggerFactory.getLogger(NonpayLoadLauncher.class);

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final Job summaryJob;
    private final Job detailJob;
    private final NonpayRepository repository;
    private final CollectorMetrics metrics;
    private final Clock clock;

    public NonpayLoadLauncher(JobOperator jobOperator, JobRepository jobRepository,
                              @Qualifier("nonpaySummaryJob") Job summaryJob,
                              @Qualifier("nonpayDetailJob") Job detailJob,
                              NonpayRepository repository, CollectorMetrics metrics, Clock clock) {
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
        this.summaryJob = summaryJob;
        this.detailJob = detailJob;
        this.repository = repository;
        this.metrics = metrics;
        this.clock = clock;
    }

    public void runIfDue() {
        String runKey = YearMonth.now(clock).toString();
        JobParameters params = new JobParametersBuilder()
                .addString("runKey", runKey)
                .toJobParameters();

        if (!runUntilComplete(summaryJob, params)) {
            return;
        }
        if (runUntilComplete(detailJob, params)) {
            log.debug("비급여 적재 {} 회차 완료 상태", runKey);
        }
    }

    /** @return 이번 회차가 완료 상태인지 (이미 완료였거나, 지금 돌려서 완료됐거나) */
    private boolean runUntilComplete(Job job, JobParameters params) {
        JobInstance instance = jobRepository.getJobInstance(job.getName(), params);
        if (instance != null) {
            JobExecution last = jobRepository.getLastJobExecution(instance);
            if (last != null && last.getStatus() == BatchStatus.COMPLETED) {
                return true;
            }
            if (last != null && last.getStatus().isRunning()) {
                log.warn("{} 실행 기록이 {} 상태로 남아 있음 (실행 중 서버 종료) → FAILED 처리 후 재시작",
                        job.getName(), last.getStatus());
                jobOperator.recover(last);
            }
        }

        boolean restart = instance != null;
        log.info("{} {} (파라미터 {})", job.getName(), restart ? "이어받기" : "시작", params);
        try {
            JobExecution execution = jobOperator.start(job, params); // SyncTaskExecutor → 끝날 때까지 기다림
            BatchStatus status = execution.getStatus();
            if (status == BatchStatus.COMPLETED) {
                long rows = NonpayBatchConfig.SUMMARY_JOB.equals(job.getName()) ? repository.countSummary() : repository.countDetail();
                log.info("{} 완료: 테이블 전체 {}건", job.getName(), rows);
                metrics.success(job.getName(), rows);
                return true;
            }
            // 한도 초과면 원인 예외가 여기 담긴다. 다음 시간(한도는 자정에 초기화)에 이어받기
            log.warn("{} 종료 상태 {}: {}", job.getName(), status,
                    execution.getAllFailureExceptions().stream().map(Throwable::getMessage).toList());
            return false;
        } catch (JobExecutionException e) {
            log.error("{} 실행 실패: {}", job.getName(), e.getMessage(), e);
            return false;
        }
    }
}
