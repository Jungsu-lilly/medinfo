package com.medinfo.publicdata;

public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(ApiOperation op, int used, int limit) {
        super("일일 호출 한도 차단: " + op + " " + used + "/" + limit);
    }
}
