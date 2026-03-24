package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.VoiceTokenResponseDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.VoiceAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceAccessService voiceAccessService;

    @PostMapping("/parties/{partyId}/token")
    public ResponseEntity<VoiceTokenResponseDTO> issuePartyVoiceToken(
            @PathVariable Long partyId,
            @AuthenticationPrincipal User user
    ) {
        VoiceTokenResponseDTO response = voiceAccessService.issuePartyVoiceToken(user, partyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
