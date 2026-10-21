package com.medinfo.er;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/**
 * 공공데이터 item 맵 → ErBedRow.
 * 응답 값이 비어 있거나 숫자가 아닐 수 있어서(예: Y/N 필드에 'N1') 숫자 필드는 실패 시 null.
 */
public final class ErBedMapper {

    private static final DateTimeFormatter HVIDATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    private ErBedMapper() {
    }

    /** hpid·기관명·입력시각이 없으면 저장할 수 없으므로 empty */
    public static Optional<ErBedRow> map(Map<String, String> item) {
        String hpid = item.get("hpid");
        String name = item.get("dutyName");
        OffsetDateTime hvidate = parseHvidate(item.get("hvidate"));
        if (isBlank(hpid) || isBlank(name) || hvidate == null) {
            return Optional.empty();
        }
        return Optional.of(new ErBedRow(
                hpid,
                name,
                item.get("dutyTel3"),
                hvidate,
                intOrNull(item.get("hvec")),
                intOrNull(item.get("hvs01")),
                intOrNull(item.get("hvoc")),
                intOrNull(item.get("hvgc")),
                intOrNull(item.get("hvicc"))
        ));
    }

    static OffsetDateTime parseHvidate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, HVIDATE).atOffset(KST);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static Integer intOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
