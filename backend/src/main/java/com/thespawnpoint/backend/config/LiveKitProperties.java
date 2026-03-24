package com.thespawnpoint.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LiveKitProperties {

    private final String url;
    private final String apiKey;
    private final String apiSecret;
    private final long tokenTtlMinutes;

    public LiveKitProperties(
            @Value("${app.livekit.url}") String url,
            @Value("${app.livekit.api-key}") String apiKey,
            @Value("${app.livekit.api-secret}") String apiSecret,
            @Value("${app.livekit.token-ttl-minutes:30}") long tokenTtlMinutes
    ) {
        this.url = url;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public String getUrl() {
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public long getTokenTtlMinutes() {
        return tokenTtlMinutes;
    }
}
