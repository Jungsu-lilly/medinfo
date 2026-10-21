package com.medinfo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 시간은 전부 이 Clock에서 얻는다 → 테스트에서 시각 고정 가능 */
    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
