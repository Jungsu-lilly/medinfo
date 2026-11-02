package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayDetailRow;

import java.util.List;

/** 상세 스텝의 처리 단위 = 병원 1곳의 상세 전체 (보통 1~2페이지) */
public record HospitalDetail(long hospitalId, List<NonpayDetailRow> rows) {
}
