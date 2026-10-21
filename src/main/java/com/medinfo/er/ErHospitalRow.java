package com.medinfo.er;

/** 국립중앙의료원 응급의료기관 목록 getEgytListInfoInqire 1건 */
public record ErHospitalRow(
        String hpid,
        String name,
        String emclsCd,   // dutyEmcls  예: G009
        String emclsNm,   // dutyEmclsName 예: 응급실운영신고기관
        String mainTel,   // dutyTel1
        String erTel,     // dutyTel3
        String addr,
        Double lon,       // wgs84Lon
        Double lat        // wgs84Lat
) {
}
