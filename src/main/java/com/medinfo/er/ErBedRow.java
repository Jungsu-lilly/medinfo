package com.medinfo.er;

import java.time.OffsetDateTime;

/** 응급실 실시간 가용병상 1건 (기관 1곳) */
public record ErBedRow(
        String hpid,
        String name,
        String erTel,
        OffsetDateTime hvidate,
        Integer hvec,   // 응급실 가용
        Integer hvs01,  // 응급실 기준병상
        Integer hvoc,   // 수술실 가용
        Integer hvgc,   // 입원실 가용
        Integer hvicc   // 일반중환자실 가용
) {
}
