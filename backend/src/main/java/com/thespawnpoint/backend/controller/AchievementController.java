package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.*;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.AchievementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/me")
    public ResponseEntity<List<AchievementDTO>> getMyAchievements(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(achievementService.getMyAchievements(user));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<AchievementDTO>> getUserAchievements(
            @PathVariable Long userId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(achievementService.getAchievementsForUser(userId, requester));
    }

    @GetMapping("/users/{userId}/collection")
    public ResponseEntity<AchievementPreviewDTO> getUserAchievementCollection(
            @PathVariable Long userId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(achievementService.getAchievementCollectionForUser(userId, requester));
    }

    @GetMapping("/users/{userId}/preview")
    public ResponseEntity<AchievementPreviewDTO> getUserAchievementPreview(
            @PathVariable Long userId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(achievementService.getAchievementPreviewForUser(userId, requester));
    }

    @PostMapping("/claim-secret")
    public ResponseEntity<AchievementDTO> claimSecret(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ClaimSecretAchievementDTO dto
    ) {
        return ResponseEntity.ok(achievementService.claimSecret(user, dto.getCode()));
    }

    @PutMapping("/me/featured")
    public ResponseEntity<AchievementPreviewDTO> updateFeaturedAchievements(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateFeaturedAchievementsDTO dto
    ) {
        return ResponseEntity.ok(achievementService.updateFeaturedAchievements(user, dto.getCodes()));
    }
}
