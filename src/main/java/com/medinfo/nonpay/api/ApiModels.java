package com.medinfo.nonpay.api;

import java.util.List;

/** 가격·병원 조회 API 응답 모양. 프론트 web/src/lib/api.ts 타입과 같게 유지 */
public final class ApiModels {

    private ApiModels() {
    }

    /** 가격 통계: 병원별 최저가 기준 */
    public record Stats(int hospitalCount, int low, int median, int high) {
    }

    /**
     * 항목 검색·홈 목록 1건.
     * vaccine = 예방접종 묶음 카드일 때만 묶음 주소(/vaccine/{vaccine}). 이때 code는 null, group은 "백신 N종", stats는 병원마다 가장 싼 제품 기준
     */
    public record ItemSummary(String code, String name, String fullName, String group, Stats stats, String vaccine) {
    }

    public record ItemInfo(String code, String name, String fullName, List<String> path) {
    }

    /**
     * 가격 비교 결과. region(시도·시군구) 또는 point(내 위치 반경) 중 하나.
     * collectedOn = 비급여 데이터를 마지막으로 받아온 날 (yyyy-MM-dd, 아직 없으면 null).
     * products = 예방접종 묶음일 때만 제품별 통계 (같은 지역 기준, 내 위치면 전국)
     */
    public record PriceList(ItemInfo item, RegionRef region, PointRef point, Stats stats, List<PriceRow> rows,
                            String collectedOn, List<ItemSummary> products) {
    }

    public record RegionRef(String sido, String sidoName, String sggu, String sgguName) {
    }

    public record PointRef(double lat, double lng, int radiusMeters) {
    }

    /** distanceMeters는 내 위치 기준일 때만, product는 예방접종 묶음일 때만 (이 병원에서 가장 싼 제품) */
    public record PriceRow(long hospitalId, String name, String kind, int minPrice, int maxPrice,
                           Integer distanceMeters, String product) {
    }

    /**
     * noPriceReason: 가격이 없을 때만. CLINIC = 의원급(심평원 미공개), NOT_REPORTED = 병원급인데 신고·공개된 가격 없음
     */
    public record HospitalPage(long id, String name, String kind, String addr, String tel, String homepage,
                               String nonpayUrl, String sidoName, String sgguName, Double lat, Double lng,
                               boolean detailed, int itemCount, List<ItemGroup> groups,
                               String noPriceReason, String collectedOn) {
    }

    public record ItemGroup(String name, List<HospitalItem> items) {
    }

    /** name = 병원이 쓰는 이름, std = 표준 항목 이름. regionMedian은 같은 시도 병원이 3곳 이상일 때만 */
    public record HospitalItem(String code, String name, String std, int price, Integer regionMedian) {
    }

    /** hasPrices = 비급여 가격이 있는 병원인지 (이름 검색엔 의원급도 나와서 구분 필요) */
    public record HospitalListItem(long id, String name, String kind, String addr, Integer distanceMeters,
                                   boolean hasPrices) {
    }

    public record Region(String code, String name, List<Sggu> sggu) {
    }

    public record Sggu(String code, String name) {
    }
}
