package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.PartyPresetDTO;
import com.thespawnpoint.backend.dto.SavePartyPresetDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.PartyPresetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/party-presets")
@RequiredArgsConstructor
public class PartyPresetController {

    private final PartyPresetService presetService;

    @GetMapping
    public ResponseEntity<List<PartyPresetDTO>> getPresets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(presetService.getPresets(user));
    }

    @PostMapping
    public ResponseEntity<PartyPresetDTO> savePreset(
            @Valid @RequestBody SavePartyPresetDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(presetService.savePreset(user, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePreset(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        presetService.deletePreset(user, id);
        return ResponseEntity.ok().build();
    }
}

