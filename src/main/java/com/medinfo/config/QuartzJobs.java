package com.medinfo.config;

import com.medinfo.er.ErBedCollector;
import com.medinfo.er.ErHospitalSyncService;
import com.medinfo.hospital.HospitalSyncService;
import com.medinfo.partition.SnapshotPartitionManager;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz 잡 클래스 모음. 실제 일은 서비스가 하고, 여기선 호출·예외 처리만.
 * DisallowConcurrentExecution: 이전 실행이 안 끝났으면 다음 실행을 겹쳐 돌리지 않음 (클러스터 전체 기준)
 */
public final class QuartzJobs {

    private static final Logger log = LoggerFactory.getLogger(QuartzJobs.class);

    private QuartzJobs() {
    }

    @DisallowConcurrentExecution
    public static class ErBedCollectJob extends QuartzJobBean {

        private final ErBedCollector collector;

        public ErBedCollectJob(ErBedCollector collector) {
            this.collector = collector;
        }

        @Override
        protected void executeInternal(JobExecutionContext context) {
            try {
                collector.collect();
            } catch (RuntimeException e) {
                // 실패해도 다음 주기(2분 뒤)에 정상 수집. 과거분은 메꾸지 않는다.
                log.error("응급실 수집 실패: {}", e.getMessage(), e);
            }
        }
    }

    @DisallowConcurrentExecution
    public static class PartitionMaintenanceJob extends QuartzJobBean {

        private final SnapshotPartitionManager partitionManager;

        public PartitionMaintenanceJob(SnapshotPartitionManager partitionManager) {
            this.partitionManager = partitionManager;
        }

        @Override
        protected void executeInternal(JobExecutionContext context) {
            try {
                partitionManager.maintain();
            } catch (RuntimeException e) {
                log.error("파티션 관리 실패: {}", e.getMessage(), e);
            }
        }
    }

    @DisallowConcurrentExecution
    public static class HospitalSyncJob extends QuartzJobBean {

        private final HospitalSyncService service;

        public HospitalSyncJob(HospitalSyncService service) {
            this.service = service;
        }

        @Override
        protected void executeInternal(JobExecutionContext context) {
            try {
                service.sync();
            } catch (RuntimeException e) {
                // 페이지 단위로 커밋되므로 앞부분은 남는다. 다음 주기(또는 재기동)에 처음부터 다시 upsert
                log.error("병원정보 동기화 실패: {}", e.getMessage(), e);
            }
        }
    }

    @DisallowConcurrentExecution
    public static class ErHospitalSyncJob extends QuartzJobBean {

        private final ErHospitalSyncService service;

        public ErHospitalSyncJob(ErHospitalSyncService service) {
            this.service = service;
        }

        @Override
        protected void executeInternal(JobExecutionContext context) {
            try {
                service.sync();
            } catch (RuntimeException e) {
                log.error("응급기관 목록 동기화 실패: {}", e.getMessage(), e);
            }
        }
    }
}
