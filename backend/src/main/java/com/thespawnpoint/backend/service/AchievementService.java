package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.AchievementDTO;
import com.thespawnpoint.backend.dto.AchievementPreviewDTO;
import com.thespawnpoint.backend.dto.AchievementUnlockedEventDTO;
import com.thespawnpoint.backend.entity.achievement.AchievementType;
import com.thespawnpoint.backend.entity.achievement.UserAchievement;
import com.thespawnpoint.backend.entity.user.PrivacySettings;
import com.thespawnpoint.backend.entity.user.Profile;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.entity.user.VisibilityLevel;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.FriendshipRepository;
import com.thespawnpoint.backend.repository.MessageRepository;
import com.thespawnpoint.backend.repository.PartyMemberRepository;
import com.thespawnpoint.backend.repository.PartyRequestRepository;
import com.thespawnpoint.backend.repository.PrivacySettingsRepository;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.UserAchievementRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementCatalog achievementCatalog;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final FriendshipRepository friendshipRepository;
    private final MessageRepository messageRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyRequestRepository partyRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<AchievementDTO> getMyAchievements(User user) {
        syncCalculatedAchievements(user);
        return buildAchievementList(user.getId());
    }

    public List<AchievementDTO> getAchievementsForUser(Long targetUserId, User requester) {
        ensureAchievementsVisible(targetUserId, requester);
        return buildAchievementList(targetUserId);
    }

    public AchievementPreviewDTO getAchievementPreviewForUser(Long targetUserId, User requester) {
        ensureAchievementsVisible(targetUserId, requester);

        List<AchievementDTO> all = buildAchievementList(targetUserId);
        List<AchievementDTO> featured = all.stream()
                .filter(AchievementDTO::isUnlocked)
                .filter(item -> item.getFeaturedPosition() != null)
                .sorted(Comparator.comparing(AchievementDTO::getFeaturedPosition))
                .limit(4)
                .toList();

        List<AchievementDTO> items = featured.isEmpty()
                ? all.stream()
                        .filter(AchievementDTO::isUnlocked)
                        .sorted(Comparator.comparing(AchievementDTO::getUnlockedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(4)
                        .toList()
                : featured;

        int unlockedCount = (int) all.stream().filter(AchievementDTO::isUnlocked).count();

        return AchievementPreviewDTO.builder()
                .totalCount(all.size())
                .unlockedCount(unlockedCount)
                .items(items)
                .build();
    }

    public AchievementPreviewDTO getAchievementCollectionForUser(Long targetUserId, User requester) {
        ensureAchievementsVisible(targetUserId, requester);

        List<AchievementDTO> all = buildAchievementList(targetUserId);
        List<AchievementDTO> items = all.stream()
                .filter(AchievementDTO::isUnlocked)
                .sorted(Comparator.comparingInt(AchievementDTO::getOrder))
                .toList();

        return AchievementPreviewDTO.builder()
                .totalCount(all.size())
                .unlockedCount(items.size())
                .items(items)
                .build();
    }

    @Transactional
    public AchievementPreviewDTO updateFeaturedAchievements(User user, List<String> codes) {
        syncCalculatedAchievements(user);

        List<String> orderedCodes = normalizeFeaturedCodes(codes);
        for (String code : orderedCodes) {
            achievementCatalog.findByCode(code)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Achievement not found"));
        }

        if (!orderedCodes.isEmpty()) {
            List<UserAchievement> unlockedRows = userAchievementRepository.findByUserIdAndAchievementCodeIn(user.getId(), orderedCodes);
            if (unlockedRows.size() != orderedCodes.size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only unlocked achievements can be featured");
            }

            userAchievementRepository.clearFeaturedPositions(user.getId());

            Map<String, UserAchievement> unlockedByCode = unlockedRows.stream()
                    .collect(Collectors.toMap(UserAchievement::getAchievementCode, item -> item));

            for (int i = 0; i < orderedCodes.size(); i++) {
                unlockedByCode.get(orderedCodes.get(i)).setFeaturedPosition(i);
            }
            userAchievementRepository.saveAll(unlockedRows);
        } else {
            userAchievementRepository.clearFeaturedPositions(user.getId());
        }

        return getAchievementPreviewForUser(user.getId(), user);
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
    public void syncCalculatedAchievements(User user) {
        syncRegistrationAchievement(user);
        syncProfileCompletion(user);
        syncFriendMilestones(user);
        syncMessageMilestones(user);
        syncCreatedPartyMilestones(user);
        syncJoinedPartyMilestones(user);
        syncCompletedPartyMilestones(user);
    }

    @Transactional
    public void syncRegistrationAchievement(User user) {
        if (user.isEmailVerified()) {
            unlock(user, AchievementCatalog.WELCOME_ABOARD, "AUTO");
        }
    }

    @Transactional
    public void syncProfileCompletion(User user) {
        profileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            if (isProfileComplete(profile)) {
                unlock(user, AchievementCatalog.PROFILE_COMPLETED, "AUTO");
            }
        });
    }

    public boolean isProfileComplete(Profile profile) {
        if (profile.getFullName() == null || profile.getFullName().isBlank()) return false;
        if (profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) return false;
        if (profile.getBio() == null || profile.getBio().isBlank()) return false;
        if (profile.getBirthDate() == null) return false;
        if (profile.getPlatforms() == null || profile.getPlatforms().isEmpty()) return false;
        if (profile.getSkillLevel() == null) return false;
        if (profile.getPlayStyle() == null) return false;
        if (profile.getLanguages() == null || profile.getLanguages().isEmpty()) return false;
        if (profile.getCountry() == null || profile.getCountry().isBlank()) return false;
        if (profile.getRegion() == null) return false;
        return true;
    }

    @Transactional
    public void syncFriendMilestones(User user) {
        long friendCount = friendshipRepository.countByUserId(user.getId());
        unlockIfReached(user, AchievementCatalog.FIRST_FRIEND, friendCount, 1);
        unlockIfReached(user, AchievementCatalog.FRIENDS_25, friendCount, 25);
        unlockIfReached(user, AchievementCatalog.FRIENDS_50, friendCount, 50);
    }

    @Transactional
    public void syncMessageMilestones(User user) {
        long sentMessages = messageRepository.countEligibleBySenderId(user.getId());
        unlockIfReached(user, AchievementCatalog.FIRST_CHAT_MESSAGE, sentMessages, 1);
        unlockIfReached(user, AchievementCatalog.CHATTERBOX_100, sentMessages, 100);
        unlockIfReached(user, AchievementCatalog.CHATTERBOX_1000, sentMessages, 1000);
        unlockIfReached(user, AchievementCatalog.CHATTERBOX_10000, sentMessages, 10000);
    }


    @Transactional
    public void syncCreatedPartyMilestones(User user) {
        long createdParties = partyRequestRepository.countByCreatorId(user.getId());
        unlockIfReached(user, AchievementCatalog.CREATED_PARTIES_25, createdParties, 25);
        unlockIfReached(user, AchievementCatalog.CREATED_PARTIES_50, createdParties, 50);
    }

    @Transactional
    public void syncJoinedPartyMilestones(User user) {
        long joinedParties = partyMemberRepository.countAllPartiesJoinedByUserId(user.getId());
        unlockIfReached(user, AchievementCatalog.JOINED_PARTIES_25, joinedParties, 25);
        unlockIfReached(user, AchievementCatalog.JOINED_PARTIES_50, joinedParties, 50);
    }

    @Transactional
    public void syncCompletedPartyMilestones(User user) {
        long completedParties = partyMemberRepository.countCompletedPartiesByUserId(user.getId());
        unlockIfReached(user, AchievementCatalog.COMPLETED_PARTIES_25, completedParties, 25);
        unlockIfReached(user, AchievementCatalog.COMPLETED_PARTIES_50, completedParties, 50);
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

    private void unlockIfReached(User user, String code, long currentValue, int target) {
        if (currentValue >= target) {
            unlock(user, code, "AUTO");
        }
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

        UserProgressSnapshot progressSnapshot = loadProgressSnapshot(userId);

        return achievementCatalog.getAll().stream()
                .sorted(Comparator.comparingInt(AchievementCatalog.AchievementDefinition::getOrder))
                .map(def -> {
                    UserAchievement unlocked = unlockedByCode.get(def.getCode());
                    AchievementProgress progress = resolveProgress(def, unlocked != null, progressSnapshot);

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
                            .currentProgress(progress.currentProgress())
                            .targetProgress(progress.targetProgress())
                            .progressPercent(progress.progressPercent())
                            .showProgress(progress.showProgress())
                            .featuredPosition(unlocked != null ? unlocked.getFeaturedPosition() : null)
                            .build();
                })
                .toList();
    }

    private List<String> normalizeFeaturedCodes(List<String> codes) {
        if (codes == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Achievement codes are required");
        }
        if (codes.size() > 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can feature up to 4 achievements");
        }

        Set<String> uniqueCodes = new LinkedHashSet<>();
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Achievement code cannot be blank");
            }
            uniqueCodes.add(code.trim());
        }

        if (uniqueCodes.size() != codes.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Featured achievements must be unique");
        }

        return List.copyOf(uniqueCodes);
    }

    private UserProgressSnapshot loadProgressSnapshot(Long userId) {
        long friendCount = friendshipRepository.countByUserId(userId);
        long sentMessageCount = messageRepository.countEligibleBySenderId(userId);
        long createdPartyCount = partyRequestRepository.countByCreatorId(userId);
        long joinedPartyCount = partyMemberRepository.countAllPartiesJoinedByUserId(userId);
        long completedPartyCount = partyMemberRepository.countCompletedPartiesByUserId(userId);
        return new UserProgressSnapshot(friendCount, sentMessageCount, createdPartyCount, joinedPartyCount, completedPartyCount);
    }

    private AchievementProgress resolveProgress(
            AchievementCatalog.AchievementDefinition definition,
            boolean unlocked,
            UserProgressSnapshot progressSnapshot
    ) {
        if (!definition.hasDynamicProgress()) {
            return AchievementProgress.empty();
        }

        long actualProgress = switch (definition.getProgressMetric()) {
            case FRIEND_COUNT -> progressSnapshot.friendCount();
            case SENT_MESSAGE_COUNT -> progressSnapshot.sentMessageCount();
            case CREATED_PARTY_COUNT -> progressSnapshot.createdPartyCount();
            case JOINED_PARTY_COUNT -> progressSnapshot.joinedPartyCount();
            case COMPLETED_PARTY_COUNT -> progressSnapshot.completedPartyCount();
            case NONE -> 0L;
        };

        int target = definition.getTargetProgress();
        int visibleCurrent = unlocked
                ? target
                : (int) Math.min(actualProgress, target);
        int percent = target > 0
                ? Math.min(100, (int) Math.round((visibleCurrent * 100.0) / target))
                : 0;

        return new AchievementProgress(
                visibleCurrent,
                target,
                percent,
                definition.showsProgressBar()
        );
    }

    private record UserProgressSnapshot(
            long friendCount,
            long sentMessageCount,
            long createdPartyCount,
            long joinedPartyCount,
            long completedPartyCount
    ) {}

    private record AchievementProgress(Integer currentProgress, Integer targetProgress, Integer progressPercent, boolean showProgress) {
        private static AchievementProgress empty() {
            return new AchievementProgress(null, null, null, false);
        }
    }
}
