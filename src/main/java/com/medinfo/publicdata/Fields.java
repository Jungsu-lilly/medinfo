package com.medinfo.publicdata;

/** 공공데이터 item 값 변환 공통. 값이 비어 있거나 형식이 틀리면 null */
public final class Fields {

    private Fields() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String textOrNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    public static Integer intOrNull(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double doubleOrNull(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            double v = Double.parseDouble(s.trim());
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 한국 범위를 벗어난 좌표(0, 뒤바뀐 위경도 등)는 버린다 */
    public static boolean isKoreaCoordinate(Double lon, Double lat) {
        return lon != null && lat != null
                && lon >= 124.0 && lon <= 132.0
                && lat >= 33.0 && lat <= 39.5;
    }
}
