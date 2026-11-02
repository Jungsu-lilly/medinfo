package com.medinfo.nonpay;

import com.medinfo.publicdata.Fields;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/** 비급여 API item(태그 → 값) → 행. 필수값이 없거나 형식이 틀리면 empty (그 건만 건너뜀) */
public final class NonpayMapper {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private NonpayMapper() {
    }

    public static Optional<NonpaySummaryRow> summary(Map<String, String> item) {
        String ykiho = Fields.textOrNull(item.get("ykiho"));
        String name = Fields.textOrNull(item.get("yadmNm"));
        String clCd = Fields.textOrNull(item.get("clCd"));
        String clCdNm = Fields.textOrNull(item.get("clCdNm"));
        String sidoCd = Fields.textOrNull(item.get("sidoCd"));
        String sidoNm = Fields.textOrNull(item.get("sidoCdNm"));
        String sgguCd = Fields.textOrNull(item.get("sgguCd"));
        String sgguNm = Fields.textOrNull(item.get("sgguCdNm"));
        String npayCd = Fields.textOrNull(item.get("npayCd"));
        String korNm = Fields.textOrNull(item.get("npayKorNm"));
        LocalDate from = date(item.get("adtFrDd"));
        LocalDate to = date(item.get("adtEndDd"));
        Integer min = won(item.get("minPrc"));
        Integer max = won(item.get("maxPrc"));
        if (ykiho == null || name == null || clCd == null || clCdNm == null
                || sidoCd == null || sidoNm == null || sgguCd == null || sgguNm == null
                || npayCd == null || korNm == null || from == null || min == null || max == null) {
            return Optional.empty();
        }
        return Optional.of(new NonpaySummaryRow(
                ykiho, name, clCd, clCdNm, sidoCd, sidoNm, sgguCd, sgguNm,
                Fields.textOrNull(item.get("urlAddr")),
                npayCd, korNm,
                Fields.textOrNull(item.get("npayMdivCd")), Fields.textOrNull(item.get("npayMdivCdNm")),
                Fields.textOrNull(item.get("npaySdivCd")), Fields.textOrNull(item.get("npaySdivCdNm")),
                Fields.textOrNull(item.get("npayDtlDivCd")), Fields.textOrNull(item.get("npayDtlDivCdNm")),
                from, to == null ? LocalDate.of(9999, 12, 31) : to,
                Math.min(min, max), Math.max(min, max) // 최소·최대가 뒤바뀌어 신고된 경우 바로잡음
        ));
    }

    public static Optional<NonpayDetailRow> detail(Map<String, String> item) {
        Integer sno = Fields.intOrNull(item.get("sno"));
        String npayCd = Fields.textOrNull(item.get("npayCd"));
        LocalDate from = date(item.get("adtFrDd"));
        LocalDate to = date(item.get("adtEndDd"));
        Integer amount = won(item.get("curAmt"));
        if (sno == null || npayCd == null || from == null || amount == null) {
            return Optional.empty();
        }
        return Optional.of(new NonpayDetailRow(
                sno, npayCd, Fields.textOrNull(item.get("yadmNpayCdNm")),
                from, to == null ? LocalDate.of(9999, 12, 31) : to, amount));
    }

    /** "20260903" → 2026-09-03 */
    static LocalDate date(String s) {
        if (Fields.isBlank(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim(), YYYYMMDD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** "380000", "380000.0" 모두 허용. 음수·int 범위 밖은 버린다 */
    static Integer won(String s) {
        if (Fields.isBlank(s)) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(s.trim().replace(",", "")).setScale(0, RoundingMode.HALF_UP);
            if (v.signum() < 0 || v.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
                return null;
            }
            return v.intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }
}
