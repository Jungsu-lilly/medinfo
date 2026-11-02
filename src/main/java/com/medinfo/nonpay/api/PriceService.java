package com.medinfo.nonpay.api;

import com.medinfo.er.api.DisplayNames;
import com.medinfo.nonpay.api.ApiModels.HospitalItem;
import com.medinfo.nonpay.api.ApiModels.HospitalListItem;
import com.medinfo.nonpay.api.ApiModels.HospitalPage;
import com.medinfo.nonpay.api.ApiModels.ItemGroup;
import com.medinfo.nonpay.api.ApiModels.ItemInfo;
import com.medinfo.nonpay.api.ApiModels.ItemSummary;
import com.medinfo.nonpay.api.ApiModels.PointRef;
import com.medinfo.nonpay.api.ApiModels.PriceList;
import com.medinfo.nonpay.api.ApiModels.PriceRow;
import com.medinfo.nonpay.api.ApiModels.Region;
import com.medinfo.nonpay.api.ApiModels.RegionRef;
import com.medinfo.nonpay.api.ApiModels.Sggu;
import com.medinfo.nonpay.api.ApiModels.Stats;
import com.medinfo.nonpay.api.PriceQueryRepository.HospitalItemRaw;
import com.medinfo.nonpay.api.PriceQueryRepository.HospitalListRaw;
import com.medinfo.nonpay.api.PriceQueryRepository.ItemRow;
import com.medinfo.nonpay.api.PriceQueryRepository.ItemStatRow;
import com.medinfo.nonpay.api.PriceQueryRepository.PriceRaw;
import com.medinfo.nonpay.api.PriceQueryRepository.RegionRaw;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 조회 결과(raw)를 화면용 모양으로. 계산(통계·그룹·이름 정리)은 여기서 */
@Service
public class PriceService {

    /** 지역 중간값은 비교 대상 병원이 이만큼은 있어야 보여준다 (2곳 중간값은 의미가 약함) */
    static final int MIN_HOSPITALS_FOR_MEDIAN = 3;
    static final String OTHER_GROUP = "기타";
    static final String NO_PRICE_CLINIC = "CLINIC";
    static final String NO_PRICE_NOT_REPORTED = "NOT_REPORTED";
    private static final int MAX_ROWS = 500;

    private final PriceQueryRepository repository;
    private final List<String> featuredCodes;

    public PriceService(PriceQueryRepository repository,
                        @Value("${nonpay.featured-codes:}") List<String> featuredCodes) {
        this.repository = repository;
        this.featuredCodes = featuredCodes.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public List<ItemSummary> search(String query, int limit) {
        return repository.searchItems(ItemNames.terms(query), limit).stream().map(PriceService::toSummary).toList();
    }

    /** 홈 "많이 찾는 비급여": 설정한 코드 순서대로, 설정이 없으면 병원이 많은 항목 */
    public List<ItemSummary> featured(String sido, int limit) {
        List<ItemStatRow> rows = featuredCodes.isEmpty()
                ? repository.topItems(sido, limit)
                : repository.itemsByCodes(featuredCodes, sido);
        return rows.stream().limit(limit).map(PriceService::toSummary).toList();
    }

    public Optional<PriceList> pricesInRegion(String code, String sido, String sggu) {
        return repository.findItem(code).map(item -> {
            RegionRef region = repository.region(sido, sggu)
                    .map(r -> new RegionRef(r.sidoCd(), r.sidoNm(), sggu == null ? null : r.sgguCd(),
                            sggu == null ? null : r.sgguNm()))
                    .orElse(new RegionRef(sido, null, sggu, null));
            List<PriceRaw> raw = repository.pricesInRegion(code, sido, sggu, MAX_ROWS);
            return toPriceList(item, region, null, raw, collectedOn());
        });
    }

    public Optional<PriceList> pricesNear(String code, double lat, double lng, int radius) {
        return repository.findItem(code).map(item -> toPriceList(item, null, new PointRef(lat, lng, radius),
                repository.pricesNear(code, lat, lng, radius, MAX_ROWS), collectedOn()));
    }

    public Optional<HospitalPage> hospital(long id) {
        return repository.findHospital(id).map(h -> {
            List<HospitalItemRaw> raw = repository.hospitalDetailItems(id, h.sidoCd());
            boolean detailed = !raw.isEmpty();
            if (!detailed) {
                raw = repository.hospitalSummaryItems(id, h.sidoCd());
            }
            List<ItemGroup> groups = group(raw);
            String noPriceReason = raw.isEmpty() ? noPriceReason(h.kind()) : null;
            return new HospitalPage(h.id(), DisplayNames.clean(h.name()), h.kind(), h.addr(), h.tel(),
                    h.hospUrl(), h.nonpayUrl(), h.sidoNm(), h.sgguNm(), h.lat(), h.lng(),
                    detailed, raw.size(), groups, noPriceReason, collectedOn());
        });
    }

    public List<HospitalListItem> hospitalsNear(double lat, double lng, int radius, int limit) {
        return repository.hospitalsNear(lat, lng, radius, limit).stream().map(PriceService::toListItem).toList();
    }

    public List<HospitalListItem> hospitalsByName(String query, int limit) {
        return repository.hospitalsByName(query, limit).stream().map(PriceService::toListItem).toList();
    }

    public List<Region> regions() {
        Map<String, Region> bySido = new LinkedHashMap<>();
        for (RegionRaw r : repository.regions()) {
            bySido.computeIfAbsent(r.sidoCd(), k -> new Region(r.sidoCd(), r.sidoNm(), new ArrayList<>()))
                    .sggu().add(new Sggu(r.sgguCd(), r.sgguNm()));
        }
        return List.copyOf(bySido.values());
    }

    // ── 변환 ─────────────────────────────────────────

    static ItemSummary toSummary(ItemStatRow r) {
        return new ItemSummary(r.code(), ItemNames.shortName(r.korNm()), r.korNm(), r.mdivNm(),
                new Stats(r.hospitalCount(), r.low(), r.median(), r.high()));
    }

    private String collectedOn() {
        return repository.lastNonpayCollectedOn().map(LocalDate::toString).orElse(null);
    }

    /**
     * 가격이 없는 이유. 종별 이름으로 판단한다 (심평원 clCdNm: 의원, 치과의원, 한의원, 보건소, 보건지소, 보건진료소, 조산원 …).
     * 의원급 비급여는 심평원이 오픈API로 공개하지 않는다 (2026-10-15 심평원 답변)
     */
    static String noPriceReason(String kind) {
        if (kind != null && (kind.endsWith("의원") || kind.startsWith("보건") || kind.equals("조산원"))) {
            return NO_PRICE_CLINIC;
        }
        return NO_PRICE_NOT_REPORTED;
    }

    static PriceList toPriceList(ItemRow item, RegionRef region, PointRef point, List<PriceRaw> raw,
                                 String collectedOn) {
        List<PriceRow> rows = raw.stream()
                .map(p -> new PriceRow(p.hospitalId(), DisplayNames.clean(p.name()), p.kind(),
                        p.minPrice(), p.maxPrice(),
                        p.distance() == null ? null : (int) Math.round(p.distance())))
                .toList();
        ItemInfo info = new ItemInfo(item.code(), ItemNames.shortName(item.korNm()), item.korNm(),
                ItemNames.path(item.mdivNm(), item.sdivNm(), item.dtlDivNm()));
        return new PriceList(info, region, point, stats(rows.stream().map(PriceRow::minPrice).toList()), rows,
                collectedOn);
    }

    /** 병원별 최저가 목록 → 병원 수·최저·중간·최고. 비어 있으면 전부 0 */
    static Stats stats(List<Integer> prices) {
        if (prices.isEmpty()) {
            return new Stats(0, 0, 0, 0);
        }
        List<Integer> sorted = prices.stream().sorted().toList();
        int n = sorted.size();
        int median = n % 2 == 1 ? sorted.get(n / 2)
                : (int) Math.round((sorted.get(n / 2 - 1) + (long) sorted.get(n / 2)) / 2.0);
        return new Stats(n, sorted.get(0), median, sorted.get(n - 1));
    }

    /** 중분류별로 묶는다. 항목이 많은 분류부터, 분류가 없는 항목은 "기타"로 맨 뒤 */
    static List<ItemGroup> group(List<HospitalItemRaw> raw) {
        Map<String, List<HospitalItem>> groups = new LinkedHashMap<>();
        List<HospitalItem> others = new ArrayList<>();
        for (HospitalItemRaw r : raw) {
            Integer median = r.hospitalCount() != null && r.hospitalCount() >= MIN_HOSPITALS_FOR_MEDIAN
                    ? r.median() : null;
            HospitalItem item = new HospitalItem(r.code(), r.name(), ItemNames.shortName(r.korNm()), r.price(), median);
            if (r.mdivNm() == null || r.mdivNm().isBlank()) {
                others.add(item);
            } else {
                groups.computeIfAbsent(r.mdivNm().trim(), k -> new ArrayList<>()).add(item);
            }
        }
        List<ItemGroup> out = new ArrayList<>();
        groups.forEach((name, items) -> out.add(new ItemGroup(name, items)));
        out.sort((a, b) -> Integer.compare(b.items().size(), a.items().size())); // 항목 많은 분류부터 (같으면 이름순 유지)
        if (!others.isEmpty()) {
            out.add(new ItemGroup(OTHER_GROUP, others));
        }
        return out;
    }

    static HospitalListItem toListItem(HospitalListRaw r) {
        return new HospitalListItem(r.id(), DisplayNames.clean(r.name()), r.kind(), r.addr(),
                r.distance() == null ? null : (int) Math.round(r.distance()), r.hasPrices());
    }
}
