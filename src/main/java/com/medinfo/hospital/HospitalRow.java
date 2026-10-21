package com.medinfo.hospital;

/** 심평원 병원정보서비스 getHospBasisList 1건 */
public record HospitalRow(
        String ykiho,
        String name,
        String clCd,
        String clCdNm,
        String sidoCd,
        String sidoNm,
        String sgguCd,
        String sgguNm,
        String addr,
        Double lon,   // XPos
        Double lat,   // YPos
        String tel,
        String hospUrl
) {
}
