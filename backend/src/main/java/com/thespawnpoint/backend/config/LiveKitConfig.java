package com.thespawnpoint.backend.config;

import io.livekit.server.RoomServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {

    @Bean
    public RoomServiceClient roomServiceClient(LiveKitProperties liveKitProperties) {
        return RoomServiceClient.createClient(
                normalizeServerApiUrl(liveKitProperties.getUrl()),
                liveKitProperties.getApiKey(),
                liveKitProperties.getApiSecret()
        );
    }

    private String normalizeServerApiUrl(String rawUrl) {
        String url = rawUrl == null ? "" : rawUrl.trim();

        if (url.startsWith("wss://")) {
            url = "https://" + url.substring(6);
        } else if (url.startsWith("ws://")) {
            url = "http://" + url.substring(5);
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        return url;
    }
}
