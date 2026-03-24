package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoiceTokenResponseDTO {
    private String serverUrl;
    private String participantToken;
    private String roomName;
    private String participantIdentity;
    private String participantName;
}
