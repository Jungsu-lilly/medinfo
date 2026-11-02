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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * 같은 예방접종 제품이 2개 이상 걸리면 묶음 카드 1개로 합친다 ("독감" → 백신 9종 대신 "독감 예방접종" 1줄).
     * 묶음 카드의 group 자리엔 분류 대신 "백신 N종"을 넣는다 (화면에 분류명 자리에 그대로 보임)
     * 제품 1개만 걸리면(예: "싱그릭스") 그 제품을 그대로 보여준다
     */
    public List<ItemSummary> search(String query, int limit) {
        List<ItemStatRow> rows = repository.searchItems(ItemNames.terms(query), ItemNames.excludes(query), limit);
        return groupVaccines(rows, g -> {
            List<String> codes = vaccineCodes(g);
            return new ItemSummary(null, g.label(), VaccineGroups.CATEGORY + "/" + g.disease(),
                    "백신 " + codes.size() + "종", repository.combinedStats(codes), g.slug());
        });
    }

    /** rows 순서(병원 많은 순)를 지키되, 묶음 카드는 병원 수 기준으로 다시 자리 잡는다 */
    static List<ItemSummary> groupVaccines(List<ItemStatRow> rows,
                                           Function<VaccineGroups.Group, ItemSummary> groupCard) {
        Map<VaccineGroups.Group, Long> matched = rows.stream()
                .flatMap(r -> VaccineGroups.of(r.korNm()).stream())
                .collect(Collectors.groupingBy(g -> g, LinkedHashMap::new, Collectors.counting()));
        List<ItemSummary> out = new ArrayList<>();
        Set<VaccineGroups.Group> added = new HashSet<>();
        for (ItemStatRow r : rows) {
            Optional<VaccineGroups.Group> g = VaccineGroups.of(r.korNm()).filter(x -> matched.get(x) >= 2);
            if (g.isEmpty()) {
                out.add(toSummary(r));
            } else if (added.add(g.get())) {
                out.add(groupCard.apply(g.get()));
            }
        }
        out.sort(Comparator.comparingInt((ItemSummary x) -> x.stats().hospitalCount()).reversed());
        return out;
    }

    private List<String> vaccineCodes(VaccineGroups.Group g) {
        return repository.itemsByNamePrefix(g.korNmPrefix(), PriceQueryRepository.NATIONAL).stream()
                .map(ItemStatRow::code).toList();
    }

    /** 홈 "많이 찾는 비급여": 설정한 코드 순서대로, 설정이 없으면 병원이 많은 항목 */
    public List<ItemSummary> featured(String sido, int limit) {
        List<ItemStatRow> rows = featuredCodes.isEmpty()
                ? repository.topItems(sido, limit)
                : repository.itemsByCodes(featuredCodes, sido);
        return rows.stream().limit(limit).map(PriceService::toSummary).toList();
    }

    public Optional<PriceList> pricesInRegion(String code, String sido, String sggu) {
        return repository.findItem(code).map(item -> toPriceList(item, regionRef(sido, sggu), null,
                repository.pricesInRegion(List.of(code), sido, sggu, MAX_ROWS), collectedOn()));
    }

    public Optional<PriceList> pricesNear(String code, double lat, double lng, int radius) {
        return repository.findItem(code).map(item -> toPriceList(item, null, new PointRef(lat, lng, radius),
                repository.pricesNear(List.of(code), lat, lng, radius, MAX_ROWS), collectedOn()));
    }

    /** 예방접종 묶음: 같은 질병 제품을 합쳐 병원마다 가장 싼 제품 가격으로 비교 */
    public Optional<PriceList> vaccineInRegion(String slug, String sido, String sggu) {
        return vaccine(slug, sido, (codes) -> repository.pricesInRegion(codes, sido, sggu, MAX_ROWS),
                regionRef(sido, sggu), null);
    }

    public Optional<PriceList> vaccineNear(String slug, double lat, double lng, int radius) {
        return vaccine(slug, PriceQueryRepository.NATIONAL,
                (codes) -> repository.pricesNear(codes, lat, lng, radius, MAX_ROWS),
                null, new PointRef(lat, lng, radius));
    }

    private Optional<PriceList> vaccine(String slug, String productSido, Function<List<String>, List<PriceRaw>> prices,
                                        RegionRef region, PointRef point) {
        return VaccineGroups.bySlug(slug).flatMap(g -> {
            List<ItemStatRow> all = repository.itemsByNamePrefix(g.korNmPrefix(), PriceQueryRepository.NATIONAL);
            if (all.isEmpty()) {
                return Optional.empty();
            }
            List<ItemStatRow> local = productSido.equals(PriceQueryRepository.NATIONAL) ? all
                    : repository.itemsByNamePrefix(g.korNmPrefix(), productSido);
            List<PriceRaw> raw = prices.apply(all.stream().map(ItemStatRow::code).toList());
            return Optional.of(toVaccineList(g, all, local, region, point, raw, collectedOn()));
        });
    }

    private RegionRef regionRef(String sido, String sggu) {
        return repository.region(sido, sggu)
                .map(r -> new RegionRef(r.sidoCd(), r.sidoNm(), sggu == null ? null : r.sgguCd(),
                        sggu == null ? null : r.sgguNm()))
                .orElse(new RegionRef(sido, null, sggu, null));
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
                new Stats(r.hospitalCount(), r.low(), r.median(), r.high()), null);
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
        List<PriceRow> rows = toRows(raw, code -> null);
        ItemInfo info = new ItemInfo(item.code(), ItemNames.shortName(item.korNm()), item.korNm(),
                ItemNames.path(item.mdivNm(), item.sdivNm(), item.dtlDivNm()));
        return new PriceList(info, region, point, stats(rows.stream().map(PriceRow::minPrice).toList()), rows,
                collectedOn, null);
    }

    /** all = 전국 제품(이름 찾기용), local = 화면에 보여줄 제품별 통계 (같은 지역) */
    static PriceList toVaccineList(VaccineGroups.Group g, List<ItemStatRow> all, List<ItemStatRow> local,
                                   RegionRef region, PointRef point, List<PriceRaw> raw, String collectedOn) {
        Map<String, String> products = new HashMap<>();
        all.forEach(r -> products.put(r.code(), VaccineGroups.product(r.korNm())));
        List<PriceRow> rows = toRows(raw, products::get);
        ItemInfo info = new ItemInfo(g.slug(), g.label(), VaccineGroups.CATEGORY + "/" + g.disease(),
                List.of(VaccineGroups.CATEGORY, g.disease().replace("/", "·")));
        List<ItemSummary> productStats = local.stream()
                .map(r -> new ItemSummary(r.code(), VaccineGroups.product(r.korNm()), r.korNm(), r.mdivNm(),
                        new Stats(r.hospitalCount(), r.low(), r.median(), r.high()), null))
                .toList();
        return new PriceList(info, region, point, stats(rows.stream().map(PriceRow::minPrice).toList()), rows,
                collectedOn, productStats);
    }

    private static List<PriceRow> toRows(List<PriceRaw> raw, Function<String, String> product) {
        return raw.stream()
                .map(p -> new PriceRow(p.hospitalId(), DisplayNames.clean(p.name()), p.kind(),
                        p.minPrice(), p.maxPrice(),
                        p.distance() == null ? null : (int) Math.round(p.distance()), product.apply(p.code())))
                .toList();
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
