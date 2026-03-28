package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.*;
import com.thespawnpoint.backend.entity.achievement.AchievementType;
import com.thespawnpoint.backend.entity.achievement.UserAchievement;
import com.thespawnpoint.backend.entity.user.PrivacySettings;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.entity.user.VisibilityLevel;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.FriendshipRepository;
import com.thespawnpoint.backend.repository.PrivacySettingsRepository;
import com.thespawnpoint.backend.repository.UserAchievementRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementCatalog achievementCatalog;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final FriendshipRepository friendshipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<AchievementDTO> getMyAchievements(User user) {
        return buildAchievementList(user.getId());
    }

    public List<AchievementDTO> getAchievementsForUser(Long targetUserId, User requester) {
        ensureAchievementsVisible(targetUserId, requester);
        return buildAchievementList(targetUserId);
    }

    public AchievementPreviewDTO getAchievementPreviewForUser(Long targetUserId, User requester) {
        ensureAchievementsVisible(targetUserId, requester);

        List<AchievementDTO> all = buildAchievementList(targetUserId);
        List<AchievementDTO> items = all.stream()
                .filter(a -> a.isUnlocked())
                .sorted(Comparator.comparing(AchievementDTO::getUnlockedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .toList();

        if (items.isEmpty() && requester != null && requester.getId().equals(targetUserId)) {
            items = all.stream().limit(4).toList();
        }

        int unlockedCount = (int) all.stream().filter(AchievementDTO::isUnlocked).count();

        return AchievementPreviewDTO.builder()
                .totalCount(all.size())
                .unlockedCount(unlockedCount)
                .items(items)
                .build();
    }

    @Transactional
    public AchievementDTO claimSecret(User user, String code) {
        AchievementCatalog.AchievementDefinition definition = achievementCatalog.findByCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Achievement not found"));

        if (definition.getType() != AchievementType.SECRET) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This achievement cannot be claimed manually");
        }

        unlock(user, code, "SECRET");
        return buildAchievementList(user.getId()).stream()
                .filter(a -> a.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Achievement not found"));
    }

    @Transactional
    public boolean unlock(User user, String code, String source) {
        AchievementCatalog.AchievementDefinition definition = achievementCatalog.findByCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Achievement not found"));

        if (userAchievementRepository.existsByUserIdAndAchievementCode(user.getId(), code)) {
            return false;
        }

        UserAchievement saved = userAchievementRepository.save(UserAchievement.builder()
                .user(user)
                .achievementCode(code)
                .source(source)
                .build());

        AchievementUnlockedEventDTO event = AchievementUnlockedEventDTO.builder()
                .code(definition.getCode())
                .title(definition.getTitle())
                .description(definition.getDescription())
                .type(definition.getType().name())
                .icon(definition.getIcon())
                .unlockedAt(saved.getUnlockedAt())
                .build();

        messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/achievements", event);
        return true;
    }

    private void ensureAchievementsVisible(Long targetUserId, User requester) {
        boolean isOwner = requester != null && requester.getId().equals(targetUserId);
        if (isOwner) {
            return;
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (targetUser.getRole().name().equals("ADMIN")) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }

        PrivacySettings settings = privacySettingsRepository.findByUserId(targetUserId).orElse(null);
        VisibilityLevel visibility = settings != null ? settings.getAchievementsVisibility() : VisibilityLevel.ALL;

        boolean isFriend = requester != null && friendshipRepository.areFriends(requester.getId(), targetUserId);
        boolean visible = switch (visibility) {
            case ALL -> true;
            case FRIENDS -> isFriend;
            case NOBODY -> false;
        };

        if (!visible) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Achievements are hidden");
        }
    }

    private List<AchievementDTO> buildAchievementList(Long userId) {
        Map<String, UserAchievement> unlockedByCode = userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(
                        UserAchievement::getAchievementCode,
                        ua -> ua,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return achievementCatalog.getAll().stream()
                .sorted(Comparator.comparingInt(AchievementCatalog.AchievementDefinition::getOrder))
                .map(def -> {
                    UserAchievement unlocked = unlockedByCode.get(def.getCode());
                    return AchievementDTO.builder()
                            .code(def.getCode())
                            .title(def.getTitle())
                            .description(def.getDescription())
                            .type(def.getType().name())
                            .icon(def.getIcon())
                            .requirementText(def.getRequirementText())
                            .secretHint(def.getSecretHint())
                            .hiddenBeforeUnlock(def.isHiddenBeforeUnlock())
                            .unlocked(unlocked != null)
                            .unlockedAt(unlocked != null ? unlocked.getUnlockedAt() : null)
                            .order(def.getOrder())
                            .build();
                })
                .toList();
    }
}
