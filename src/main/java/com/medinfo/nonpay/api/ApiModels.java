package com.medinfo.nonpay.api;

import java.util.List;

/** 가격·병원 조회 API 응답 모양. 프론트 web/src/lib/api.ts 타입과 같게 유지 */
public final class ApiModels {

    private ApiModels() {
    }

    /** 가격 통계: 병원별 최저가 기준 */
    public record Stats(int hospitalCount, int low, int median, int high) {
    }

    /** 항목 검색·홈 목록 1건 */
    public record ItemSummary(String code, String name, String fullName, String group, Stats stats) {
    }

    public record ItemInfo(String code, String name, String fullName, List<String> path) {
    }

    /** 가격 비교 결과. region(시도·시군구) 또는 point(내 위치 반경) 중 하나 */
    public record PriceList(ItemInfo item, RegionRef region, PointRef point, Stats stats, List<PriceRow> rows) {
    }

    public record RegionRef(String sido, String sidoName, String sggu, String sgguName) {
    }

    public record PointRef(double lat, double lng, int radiusMeters) {
    }

    /** distanceMeters는 내 위치 기준일 때만 */
    public record PriceRow(long hospitalId, String name, String kind, int minPrice, int maxPrice,
                           Integer distanceMeters) {
    }

    public record HospitalPage(long id, String name, String kind, String addr, String tel, String homepage,
                               String nonpayUrl, String sidoName, String sgguName, Double lat, Double lng,
                               boolean detailed, int itemCount, List<ItemGroup> groups) {
    }

    public record ItemGroup(String name, List<HospitalItem> items) {
    }

    /** name = 병원이 쓰는 이름, std = 표준 항목 이름. regionMedian은 같은 시도 병원이 3곳 이상일 때만 */
    public record HospitalItem(String code, String name, String std, int price, Integer regionMedian) {
    }

    public record HospitalListItem(long id, String name, String kind, String addr, Integer distanceMeters) {
    }

    public record Region(String code, String name, List<Sggu> sggu) {
    }

    public record Sggu(String code, String name) {
    }
}
