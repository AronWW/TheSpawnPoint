package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.PrivacySettingsDTO;
import com.thespawnpoint.backend.dto.ProfileDTO;
import com.thespawnpoint.backend.dto.UpdateProfileDTO;
import com.thespawnpoint.backend.entity.party.PartyMember;
import com.thespawnpoint.backend.entity.party.PartyStatus;
import com.thespawnpoint.backend.entity.user.*;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.FriendshipRepository;
import com.thespawnpoint.backend.repository.PartyMemberRepository;
import com.thespawnpoint.backend.repository.PrivacySettingsRepository;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final CloudinaryImageService cloudinaryImageService;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final FriendshipRepository friendshipRepository;
    private final AchievementService achievementService;

    @Value("${app.upload.max-size:2097152}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final List<String> DEFAULT_AVATARS = List.of(
            "/avatars/default/avatar-1.png",
            "/avatars/default/avatar-2.png",
            "/avatars/default/avatar-3.png",
            "/avatars/default/avatar-4.png",
            "/avatars/default/avatar-5.png",
            "/avatars/default/avatar-6.png",
            "/avatars/default/avatar-7.png",
            "/avatars/default/avatar-8.png",
            "/avatars/default/avatar-9.png",
            "/avatars/default/avatar-10.png"
    );

    private static final Set<String> ALLOWED_BANNERS = Set.of(
            "banner-1", "banner-2", "banner-3", "banner-4", "banner-5"
    );

    public ProfileDTO getMyProfile(User user) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));
        ProfileDTO dto = toDTO(profile, user);
        privacySettingsRepository.findByUserId(user.getId())
                .ifPresent(ps -> dto.setPrivacy(buildPrivacyDTO(ps)));
        return dto;
    }

    public ProfileDTO getProfileByUserId(Long userId, User requester) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (profile.getUser().getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Profile not found");
        }

        ProfileDTO dto = toDTO(profile, profile.getUser());

        boolean isOwner = requester != null && requester.getId().equals(userId);
        boolean isFriend = requester != null && !isOwner && friendshipRepository.areFriends(requester.getId(), userId);

        PrivacySettings ps = privacySettingsRepository.findByUserId(userId).orElse(null);
        if (ps != null && !isOwner) {
            if (!isVisible(ps.getFriendsVisibility(), isFriend)) {
                dto.setPrivacy(buildPrivacyDTO(ps));
            }
            if (!isVisible(ps.getStatusVisibility(), isFriend)) {
                dto.setStatus("OFFLINE");
                dto.setLastSeen(null);
            }
            if (!isVisible(ps.getFavoriteGamesVisibility(), isFriend)) {

            }
            if (!isVisible(ps.getSocialsVisibility(), isFriend)) {
                dto.setDiscord(null);
                dto.setSteam(null);
                dto.setTwitch(null);
                dto.setXbox(null);
                dto.setPlaystation(null);
                dto.setNintendo(null);
            }
            dto.setPrivacy(buildPrivacyDTO(ps, isFriend));
        } else if (ps != null) {
            dto.setPrivacy(buildPrivacyDTO(ps));
        }

        return dto;
    }

    private boolean isVisible(VisibilityLevel level, boolean isFriend) {
        return switch (level) {
            case ALL -> true;
            case FRIENDS -> isFriend;
            case NOBODY -> false;
        };
    }

    private PrivacySettingsDTO buildPrivacyDTO(PrivacySettings ps) {
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

    private PrivacySettingsDTO buildPrivacyDTO(PrivacySettings ps, boolean isFriend) {

        return PrivacySettingsDTO.builder()
                .friendsVisibility(isVisible(ps.getFriendsVisibility(), isFriend) ? "VISIBLE" : "HIDDEN")
                .statusVisibility(isVisible(ps.getStatusVisibility(), isFriend) ? "VISIBLE" : "HIDDEN")
                .favoriteGamesVisibility(isVisible(ps.getFavoriteGamesVisibility(), isFriend) ? "VISIBLE" : "HIDDEN")
                .statsVisibility(isVisible(ps.getStatsVisibility(), isFriend) ? "VISIBLE" : "HIDDEN")
                .socialsVisibility(isVisible(ps.getSocialsVisibility(), isFriend) ? "VISIBLE" : "HIDDEN")
                .commentsPolicy(isVisible(ps.getCommentsPolicy(), isFriend) ? "VISIBLE" : "HIDDEN")
                .achievementsVisibility(isVisible(ps.getAchievementsVisibility(), isFriend) ? "VISIBLE" : "HIDDEN")
                .build();
    }

    @Transactional
    public ProfileDTO updateMyProfile(User user, UpdateProfileDTO dto) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (dto.getDisplayName() != null && !dto.getDisplayName().isBlank()) {
            String newName = dto.getDisplayName().trim();
            if (!newName.equalsIgnoreCase(user.getDisplayName())) {
                if (userRepository.existsByDisplayNameIgnoreCase(newName)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Display name already taken");
                }
                user.setDisplayName(newName);
                userRepository.save(user);
            }
        }

        if (dto.getFullName() != null) profile.setFullName(dto.getFullName());
        if (dto.getBio() != null) profile.setBio(dto.getBio());
        if (dto.getBirthDate() != null) profile.setBirthDate(dto.getBirthDate());
        if (dto.getCountry() != null) profile.setCountry(dto.getCountry());
        if (dto.getDiscord() != null) profile.setDiscord(dto.getDiscord().isBlank() ? null : dto.getDiscord().trim());
        if (dto.getSteam() != null) profile.setSteam(dto.getSteam().isBlank() ? null : dto.getSteam().trim());
        if (dto.getTwitch() != null) profile.setTwitch(dto.getTwitch().isBlank() ? null : dto.getTwitch().trim());
        if (dto.getXbox() != null) profile.setXbox(dto.getXbox().isBlank() ? null : dto.getXbox().trim());
        if (dto.getPlaystation() != null) profile.setPlaystation(dto.getPlaystation().isBlank() ? null : dto.getPlaystation().trim());
        if (dto.getNintendo() != null) profile.setNintendo(dto.getNintendo().isBlank() ? null : dto.getNintendo().trim());

        if (dto.getPlatforms() != null) {
            validatePlatforms(dto.getPlatforms());
            profile.setPlatforms(dto.getPlatforms());
        }
        if (dto.getSkillLevel() != null) {
            profile.setSkillLevel(
                    dto.getSkillLevel().isBlank() ? null
                            : parseEnum(SkillLevel.class, dto.getSkillLevel(), "skill level")
            );
        }
        if (dto.getPlayStyle() != null) {
            profile.setPlayStyle(
                    dto.getPlayStyle().isBlank() ? null
                            : parseEnum(PlayStyle.class, dto.getPlayStyle(), "play style")
            );
        }
        if (dto.getRegion() != null) {
            profile.setRegion(
                    dto.getRegion().isBlank() ? null
                            : parseEnum(Region.class, dto.getRegion(), "region")
            );
        }
        if (dto.getLanguages() != null) {
            validateLanguages(dto.getLanguages());
            profile.setLanguages(dto.getLanguages());
        }
        if (dto.getBannerUrl() != null) {
            if (dto.getBannerUrl().isBlank()) {
                profile.setBannerUrl(null);
            } else if (ALLOWED_BANNERS.contains(dto.getBannerUrl())) {
                profile.setBannerUrl(dto.getBannerUrl());
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid banner: " + dto.getBannerUrl());
            }
        }

        profileRepository.save(profile);

        if (achievementService.isProfileComplete(profile)) {
            achievementService.unlock(user, AchievementCatalog.PROFILE_COMPLETED, "AUTO");
        }

        return toDTO(profile, user);
    }

    @Transactional
    public ProfileDTO uploadAvatar(User user, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File size exceeds 2 MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WebP and GIF are allowed");
        }

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (profile.getAvatarPublicId() != null && !profile.getAvatarPublicId().isBlank()) {
            cloudinaryImageService.deleteAvatar(profile.getAvatarPublicId());
        }

        CloudinaryImageService.UploadResult uploadResult =
                cloudinaryImageService.uploadAvatar(file, user.getId());

        profile.setAvatarUrl(uploadResult.secureUrl());
        profile.setAvatarPublicId(uploadResult.publicId());
        profileRepository.save(profile);

        if (achievementService.isProfileComplete(profile)) {
            achievementService.unlock(user, AchievementCatalog.PROFILE_COMPLETED, "AUTO");
        }

        return toDTO(profile, user);
    }

    @Transactional
    public ProfileDTO selectDefaultAvatar(User user, int index) {
        if (index < 1 || index > DEFAULT_AVATARS.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Avatar index must be between 1 and " + DEFAULT_AVATARS.size());
        }

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (profile.getAvatarPublicId() != null && !profile.getAvatarPublicId().isBlank()) {
            cloudinaryImageService.deleteAvatar(profile.getAvatarPublicId());
        }

        profile.setAvatarUrl(DEFAULT_AVATARS.get(index - 1));
        profile.setAvatarPublicId(null);
        profileRepository.save(profile);

        if (achievementService.isProfileComplete(profile)) {
            achievementService.unlock(user, AchievementCatalog.PROFILE_COMPLETED, "AUTO");
        }

        return toDTO(profile, user);
    }

    public List<String> getDefaultAvatars() {
        return DEFAULT_AVATARS;
    }

    private ProfileDTO toDTO(Profile profile, User user) {
        String derivedStatus = deriveStatus(user);

        return ProfileDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .birthDate(profile.getBirthDate())
                .platforms(profile.getPlatforms())
                .skillLevel(profile.getSkillLevel() != null ? profile.getSkillLevel().name() : null)
                .playStyle(profile.getPlayStyle() != null ? profile.getPlayStyle().name() : null)
                .languages(profile.getLanguages())
                .country(profile.getCountry())
                .region(profile.getRegion() != null ? profile.getRegion().name() : null)
                .discord(profile.getDiscord())
                .steam(profile.getSteam())
                .twitch(profile.getTwitch())
                .xbox(profile.getXbox())
                .playstation(profile.getPlaystation())
                .nintendo(profile.getNintendo())
                .bannerUrl(profile.getBannerUrl())
                .status(derivedStatus)
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String deriveStatus(User user) {
        if (user.getStatus() != User.Status.ONLINE) {
            return "OFFLINE";
        }
        List<PartyMember> memberships = partyMemberRepository.findByUserId(user.getId());
        boolean inGame = false;
        boolean inLobby = false;
        for (PartyMember pm : memberships) {
            PartyStatus ps = pm.getPartyRequest().getStatus();
            if (ps == PartyStatus.IN_GAME) {
                inGame = true;
            } else if (ps == PartyStatus.OPEN || ps == PartyStatus.FULL) {
                inLobby = true;
            }
        }
        if (inGame) return "IN_GAME";
        if (inLobby) return "IN_LOBBY";
        return "ONLINE";
    }

    private void validateLanguages(List<String> languages) {
        for (String lang : languages) {
            try {
                Language.valueOf(lang.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid language code: " + lang + ". Allowed: " + java.util.Arrays.toString(Language.values()));
            }
        }
    }

    private void validatePlatforms(List<String> platforms) {
        for (String p : platforms) {
            try {
                Platform.valueOf(p.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid platform: " + p + ". Allowed: " + java.util.Arrays.toString(Platform.values()));
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid " + fieldName + ": " + value);
        }
    }
}