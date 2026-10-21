package com.medinfo.publicdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "publicdata")
public record PublicDataProperties(
        String baseUrl,
        String serviceKey,
        Duration connectTimeout,
        Duration readTimeout,
        int maxRetries,
        double quotaBlockRatio
) {
    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
