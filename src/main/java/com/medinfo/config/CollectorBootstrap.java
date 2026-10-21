package com.medinfo.config;

import com.medinfo.er.ErHospitalRepository;
import com.medinfo.hospital.HospitalRepository;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 처음 기동했을 때 기준 데이터가 비어 있으면 주기를 기다리지 않고 바로 한 번 돌린다.
 * Quartz에 "지금 1회 실행"으로 넣기 때문에 기동이 막히지 않고, 클러스터에서도 한 인스턴스만 실행한다.
 */
@Component
public class CollectorBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CollectorBootstrap.class);

    private final Scheduler scheduler;
    private final HospitalRepository hospitalRepository;
    private final ErHospitalRepository erHospitalRepository;
    private final boolean enabled;

    public CollectorBootstrap(Scheduler scheduler, HospitalRepository hospitalRepository,
                              ErHospitalRepository erHospitalRepository,
                              @Value("${collector.bootstrap.enabled:true}") boolean enabled) {
        this.scheduler = scheduler;
        this.hospitalRepository = hospitalRepository;
        this.erHospitalRepository = erHospitalRepository;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) throws SchedulerException {
        if (!enabled) {
            return;
        }
        if (hospitalRepository.count() == 0) {
            log.info("병원 테이블이 비어 있어 병원정보 동기화를 바로 시작합니다 (수 분 소요)");
            scheduler.triggerJob(QuartzConfig.HOSPITAL_SYNC);
        }
        if (erHospitalRepository.count() == 0 || erHospitalRepository.countWithoutLocation() > 0) {
            log.info("응급기관 위치 정보가 없어 응급기관 목록 동기화를 바로 시작합니다");
            scheduler.triggerJob(QuartzConfig.ER_HOSPITAL_SYNC);
        }
    }
}
