package com.medinfo.er.api;

import java.util.List;

/**
 * 응급실 기준 위치를 직접 고를 때 쓰는 시도·시군구 목록.
 * PC처럼 브라우저 위치가 IP 추정이라 수 km 틀리는 경우를 위해, 시군구 중심 좌표를 기준점으로 쓴다
 */
public record ErRegion(String code, String name, List<Area> sggu) {

    /** lat·lng = 그 시군구 요양기관 좌표의 중앙값 (사람이 몰려 사는 쪽에 가깝다) */
    public record Area(String code, String name, double lat, double lng) {
    }
}
