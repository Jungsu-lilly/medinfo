package com.medinfo.er;

import com.medinfo.publicdata.Fields;

import java.util.Map;
import java.util.Optional;

public final class ErHospitalMapper {

    private ErHospitalMapper() {
    }

    public static Optional<ErHospitalRow> map(Map<String, String> item) {
        String hpid = Fields.textOrNull(item.get("hpid"));
        String name = Fields.textOrNull(item.get("dutyName"));
        if (hpid == null || name == null) {
            return Optional.empty();
        }
        Double lon = Fields.doubleOrNull(item.get("wgs84Lon"));
        Double lat = Fields.doubleOrNull(item.get("wgs84Lat"));
        if (!Fields.isKoreaCoordinate(lon, lat)) {
            lon = null;
            lat = null;
        }
        return Optional.of(new ErHospitalRow(
                hpid, name,
                Fields.textOrNull(item.get("dutyEmcls")),
                Fields.textOrNull(item.get("dutyEmclsName")),
                Fields.textOrNull(item.get("dutyTel1")),
                Fields.textOrNull(item.get("dutyTel3")),
                Fields.textOrNull(item.get("dutyAddr")),
                lon, lat
        ));
    }
}
