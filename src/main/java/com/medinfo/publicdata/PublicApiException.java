package com.medinfo.publicdata;

/**
 * 공공데이터 API가 정상 응답(resultCode 00)이 아닐 때.
 * 자주 보는 코드: 12 서비스 없음(주소 오타/게이트웨이 주소), 20 키 없음·권한 없음,
 * 22 일일 한도 초과, 23 초당 한도 초과, 30 등록 안 된 키.
 */
public class PublicApiException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    public PublicApiException(String code, String message, boolean retryable) {
        super("[" + code + "] " + message);
        this.code = code;
        this.retryable = retryable;
    }

    public String code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }

    public boolean isQuotaExceeded() {
        return "22".equals(code);
    }
}
