package com.medinfo.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * 스케줄 등록. Spring Boot가 JobDetail·Trigger 빈을 Quartz(JDBC 저장소)에 자동 등록한다.
 * misfire = DoNothing: 서버가 내려가 있던 동안 놓친 실행은 몰아서 돌리지 않음
 */
@Configuration
public class QuartzConfig {

    private static final TimeZone KST = TimeZone.getTimeZone(ClockConfig.KST);

    @Bean
    @ConditionalOnProperty(name = "collector.er-bed.enabled", havingValue = "true")
    public JobDetail erBedCollectJobDetail() {
        return JobBuilder.newJob(QuartzJobs.ErBedCollectJob.class)
                .withIdentity("erBedCollect", "collector")
                .withDescription("응급실 실시간 가용병상 수집")
                .storeDurably()
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "collector.er-bed.enabled", havingValue = "true")
    public Trigger erBedCollectTrigger(JobDetail erBedCollectJobDetail,
                                       @Value("${collector.er-bed.cron}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(erBedCollectJobDetail)
                .withIdentity("erBedCollectTrigger", "collector")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(KST)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    public static final JobKey HOSPITAL_SYNC = JobKey.jobKey("hospitalSync", "collector");
    public static final JobKey ER_HOSPITAL_SYNC = JobKey.jobKey("erHospitalSync", "collector");

    @Bean
    public JobDetail hospitalSyncJobDetail() {
        return JobBuilder.newJob(QuartzJobs.HospitalSyncJob.class)
                .withIdentity(HOSPITAL_SYNC)
                .withDescription("심평원 병원정보 전국 동기화 (약 80회 호출)")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger hospitalSyncTrigger(JobDetail hospitalSyncJobDetail,
                                       @Value("${collector.hospital-sync.cron}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(hospitalSyncJobDetail)
                .withIdentity("hospitalSyncTrigger", "collector")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(KST)
                        .withMisfireHandlingInstructionFireAndProceed()) // 놓쳤으면 한 번은 돌림 (주 1회라)
                .build();
    }

    @Bean
    public JobDetail erHospitalSyncJobDetail() {
        return JobBuilder.newJob(QuartzJobs.ErHospitalSyncJob.class)
                .withIdentity(ER_HOSPITAL_SYNC)
                .withDescription("응급의료기관 목록 동기화 + hpid↔ykiho 매칭")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger erHospitalSyncTrigger(JobDetail erHospitalSyncJobDetail,
                                         @Value("${collector.er-hospital-sync.cron}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(erHospitalSyncJobDetail)
                .withIdentity("erHospitalSyncTrigger", "collector")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(KST)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    @Bean
    public JobDetail partitionMaintenanceJobDetail() {
        return JobBuilder.newJob(QuartzJobs.PartitionMaintenanceJob.class)
                .withIdentity("partitionMaintenance", "maintenance")
                .withDescription("응급실 스냅샷 파티션 생성·삭제")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger partitionMaintenanceTrigger(JobDetail partitionMaintenanceJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(partitionMaintenanceJobDetail)
                .withIdentity("partitionMaintenanceTrigger", "maintenance")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 5 * * * ?") // 매시 5분
                        .inTimeZone(KST)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
