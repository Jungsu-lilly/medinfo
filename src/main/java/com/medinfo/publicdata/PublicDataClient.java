package com.medinfo.publicdata;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공공데이터포털 공통 클라이언트.
 *
 * 설계 포인트
 *  - 연결·읽기 타임아웃 필수: 외부 API가 느려져도 호출 스레드가 무한정 묶이지 않게 (한신평 502 장애 교훈)
 *  - 리다이렉트 추종: 포털 게이트웨이가 내부 주소로 리다이렉트하는 경우가 있어 NORMAL로 설정
 *  - 재시도: 네트워크 오류·5xx·일시적 게이트웨이 에러만, 지수 백오프. 한도 초과(22)·키 오류는 재시도 안 함
 *  - 호출 전 QuotaGuard로 일일 한도 확인
 *  - 지표: external_api_calls_total{api,result}, external_api_latency_seconds{api}
 */
@Component
public class PublicDataClient {

    private static final Logger log = LoggerFactory.getLogger(PublicDataClient.class);

    private final PublicDataProperties properties;
    private final QuotaGuard quotaGuard;
    private final MeterRegistry registry;
    private final HttpClient httpClient;

    public PublicDataClient(PublicDataProperties properties, QuotaGuard quotaGuard, MeterRegistry registry) {
        this.properties = properties;
        this.quotaGuard = quotaGuard;
        this.registry = registry;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public PublicDataResponse call(ApiOperation op, Map<String, String> params) {
        if (!properties.hasServiceKey()) {
            throw new PublicApiException("NO_KEY", "PUBLIC_DATA_SERVICE_KEY 미설정 (.env 확인)", false);
        }

        int attempt = 0;
        while (true) {
            attempt++;
            quotaGuard.acquire(op); // 재시도도 호출 1건으로 센다 (포털도 그렇게 셈)
            Timer.Sample sample = Timer.start(registry);
            try {
                PublicDataResponse response = PublicDataXmlParser.parse(send(op, params));
                record(op, "success", sample);
                return response;
            } catch (PublicApiException e) {
                record(op, "api_error_" + e.code(), sample);
                if (!e.retryable() || attempt > properties.maxRetries()) {
                    throw e;
                }
                log.warn("{} 재시도 {}/{}: {}", op, attempt, properties.maxRetries(), e.getMessage());
            } catch (IOException e) {
                record(op, "io_error", sample);
                if (attempt > properties.maxRetries()) {
                    throw new PublicApiException("IO", op + " 호출 실패: " + e.getMessage(), true);
                }
                log.warn("{} 재시도 {}/{}: {}", op, attempt, properties.maxRetries(), e.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PublicApiException("INTERRUPTED", op + " 호출 중단", false);
            }
            backoff(attempt);
        }
    }

    private byte[] send(ApiOperation op, Map<String, String> params) throws IOException, InterruptedException {
        Map<String, String> all = new LinkedHashMap<>();
        all.put("serviceKey", properties.serviceKey());
        all.putAll(params);
        String query = all.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + op.path() + "?" + query))
                .timeout(properties.readTimeout())
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 500) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private void record(ApiOperation op, String result, Timer.Sample sample) {
        sample.stop(registry.timer("external.api.latency", "api", op.name()));
        registry.counter("external.api.calls", "api", op.name(), "result", result).increment();
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(500L * (1L << (attempt - 1))); // 0.5s, 1s, 2s ...
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
