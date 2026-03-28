package com.thespawnpoint.backend.controller;
import com.thespawnpoint.backend.dto.PrivacySettingsDTO;
import com.thespawnpoint.backend.dto.UpdatePrivacySettingsDTO;
import com.thespawnpoint.backend.entity.user.PrivacySettings;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.entity.user.VisibilityLevel;
import com.thespawnpoint.backend.repository.PrivacySettingsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {
    private final PrivacySettingsRepository privacySettingsRepository;
    @GetMapping("/privacy")
    public ResponseEntity<PrivacySettingsDTO> getPrivacySettings(@AuthenticationPrincipal User user) {
        PrivacySettings ps = privacySettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefault(user));
        return ResponseEntity.ok(toDTO(ps));
    }
    @PutMapping("/privacy")
    public ResponseEntity<PrivacySettingsDTO> updatePrivacySettings(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePrivacySettingsDTO dto) {
        PrivacySettings ps = privacySettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefault(user));
        ps.setFriendsVisibility(VisibilityLevel.valueOf(dto.getFriendsVisibility()));
        ps.setStatusVisibility(VisibilityLevel.valueOf(dto.getStatusVisibility()));
        ps.setFavoriteGamesVisibility(VisibilityLevel.valueOf(dto.getFavoriteGamesVisibility()));
        ps.setStatsVisibility(VisibilityLevel.valueOf(dto.getStatsVisibility()));
        ps.setSocialsVisibility(VisibilityLevel.valueOf(dto.getSocialsVisibility()));
        ps.setCommentsPolicy(VisibilityLevel.valueOf(dto.getCommentsPolicy()));
        ps.setAchievementsVisibility(VisibilityLevel.valueOf(dto.getAchievementsVisibility()));
        privacySettingsRepository.save(ps);
        return ResponseEntity.ok(toDTO(ps));
    }
    private PrivacySettings createDefault(User user) {
        PrivacySettings ps = PrivacySettings.builder().user(user).build();
        return privacySettingsRepository.save(ps);
    }
    private PrivacySettingsDTO toDTO(PrivacySettings ps) {
        return PrivacySettingsDTO.builder()
                .friendsVisibility(ps.getFriendsVisibility().name())
                .statusVisibility(ps.getStatusVisibility().name())
                .favoriteGamesVisibility(ps.getFavoriteGamesVisibility().name())
                .statsVisibility(ps.getStatsVisibility().name())
                .socialsVisibility(ps.getSocialsVisibility().name())
                .commentsPolicy(ps.getCommentsPolicy().name())
                .achievementsVisibility(ps.getAchievementsVisibility().name())
                .build();
    }
}
