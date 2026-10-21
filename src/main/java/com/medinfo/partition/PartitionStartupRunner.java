package com.medinfo.partition;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 첫 수집 전에 오늘·내일 파티션이 있어야 하므로 기동 직후 1회 실행 */
@Component
public class PartitionStartupRunner implements ApplicationRunner {

    private final SnapshotPartitionManager partitionManager;

    public PartitionStartupRunner(SnapshotPartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        partitionManager.maintain();
    }
}
