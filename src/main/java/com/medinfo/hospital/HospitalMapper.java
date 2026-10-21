package com.medinfo.hospital;

import com.medinfo.publicdata.Fields;

import java.util.Map;
import java.util.Optional;

public final class HospitalMapper {

    private HospitalMapper() {
    }

    /** 필수값(ykiho, 이름, 종별, 시도·시군구 코드/명) 중 하나라도 없으면 empty */
    public static Optional<HospitalRow> map(Map<String, String> item) {
        String ykiho = Fields.textOrNull(item.get("ykiho"));
        String name = Fields.textOrNull(item.get("yadmNm"));
        String clCd = Fields.textOrNull(item.get("clCd"));
        String clCdNm = Fields.textOrNull(item.get("clCdNm"));
        String sidoCd = Fields.textOrNull(item.get("sidoCd"));
        String sidoNm = Fields.textOrNull(item.get("sidoCdNm"));
        String sgguCd = Fields.textOrNull(item.get("sgguCd"));
        String sgguNm = Fields.textOrNull(item.get("sgguCdNm"));
        if (ykiho == null || name == null || clCd == null || clCdNm == null
                || sidoCd == null || sidoNm == null || sgguCd == null || sgguNm == null) {
            return Optional.empty();
        }

        Double lon = Fields.doubleOrNull(item.get("XPos"));
        Double lat = Fields.doubleOrNull(item.get("YPos"));
        if (!Fields.isKoreaCoordinate(lon, lat)) {
            lon = null;
            lat = null;
        }

        return Optional.of(new HospitalRow(
                ykiho, name, clCd, clCdNm, sidoCd, sidoNm, sgguCd, sgguNm,
                Fields.textOrNull(item.get("addr")),
                lon, lat,
                Fields.textOrNull(item.get("telno")),
                Fields.textOrNull(item.get("hospUrl"))
        ));
    }
}
