package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.CreatePartyRequestDTO;
import com.thespawnpoint.backend.dto.PartyMemberDTO;
import com.thespawnpoint.backend.dto.PartyRequestDTO;
import com.thespawnpoint.backend.dto.RecentTeammateDTO;
import com.thespawnpoint.backend.dto.UserStatsDTO;
import com.thespawnpoint.backend.entity.game.Game;
import com.thespawnpoint.backend.entity.party.PartyMember;
import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.party.PartyStatus;
import com.thespawnpoint.backend.entity.user.Language;
import com.thespawnpoint.backend.entity.user.PlayStyle;
import com.thespawnpoint.backend.entity.user.Platform;
import com.thespawnpoint.backend.entity.user.Region;
import com.thespawnpoint.backend.entity.user.SkillLevel;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.entity.user.PrivacySettings;
import com.thespawnpoint.backend.entity.user.VisibilityLevel;
import com.thespawnpoint.backend.entity.chat.Chat;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PartyService {

    private static final Set<PartyStatus> ACTIVE_STATUSES =
            Set.of(PartyStatus.OPEN, PartyStatus.FULL, PartyStatus.IN_GAME);

    private final PartyRequestRepository partyRequestRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final GameRepository gameRepository;
    private final ProfileRepository profileRepository;
    private final ChatService chatService;
    private final VoiceAccessService voiceAccessService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AchievementService achievementService;
    private final BlockService blockService;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional
    public PartyRequestDTO createParty(User creator, CreatePartyRequestDTO dto) {

        if (partyMemberRepository.existsActivePartyForUser(creator.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "У вас вже є активне лобі");
        }

        Game game = gameRepository.findById(dto.getGameId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Game not found"));

        validatePlatforms(dto.getPlatform());
        validateLanguages(dto.getLanguages());
        SkillLevel skillLevel = parseEnum(SkillLevel.class, dto.getSkillLevel(), "skill level");
        PlayStyle playStyle = parseEnum(PlayStyle.class, dto.getPlayStyle(), "play style");

        int maxMembers = game.getMaxPartySize();
        if (dto.getMaxMembers() != null) {
            if (dto.getMaxMembers() < 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Мінімум 2 гравці");
            }
            if (dto.getMaxMembers() > game.getMaxPartySize()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Максимум для цієї гри — " + game.getMaxPartySize() + " гравців");
            }
            maxMembers = dto.getMaxMembers();
        }

        validateTags(dto.getTags());
        Region region = parseEnum(Region.class, dto.getRegion(), "region");

        PartyRequest party = PartyRequest.builder()
                .creator(creator)
                .game(game)
                .maxMembers(maxMembers)
                .status(PartyStatus.OPEN)
                .isOpen(true)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .eventTime(dto.getEventTime())
                .platform(dto.getPlatform())
                .languages(dto.getLanguages())
                .tags(dto.getTags())
                .region(region)
                .skillLevel(skillLevel)
                .playStyle(playStyle)
                .build();

        partyRequestRepository.save(party);

        String langLabel = (dto.getLanguages() != null && !dto.getLanguages().isEmpty())
                ? String.join("/", dto.getLanguages()) : "International";
        String chatTitle = game.getName() + " • " + langLabel;
        Chat groupChat = chatService.createGroupChat(chatTitle, creator);
        party.setChat(groupChat);
        partyRequestRepository.save(party);

        PartyMember creatorMember = PartyMember.builder()
                .partyRequest(party)
                .user(creator)
                .build();
        partyMemberRepository.save(creatorMember);

        achievementService.unlock(creator, AchievementCatalog.FIRST_PARTY_CREATED, "AUTO");
        achievementService.syncCreatedPartyMilestones(creator);

        return toDTO(party, List.of(creatorMember));
    }

    @Transactional
    public PartyRequestDTO joinParty(User user, Long partyId) {

        if (partyMemberRepository.existsActivePartyForUser(user.getId())) {
            var activeParties = partyMemberRepository.findActivePartiesByUserId(user.getId());
            boolean inGame = activeParties.stream()
                    .anyMatch(pm -> pm.getPartyRequest().getStatus() == PartyStatus.IN_GAME);
            if (inGame) {
                throw new ApiException(HttpStatus.CONFLICT, "Ви не можете змінити лобi під час гри");
            }
        }

        PartyRequest party = getPartyWithStatus(partyId, PartyStatus.OPEN);

        if (blockService.isBlockedBetween(user.getId(), party.getCreator().getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Party not found");
        }

        if (partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ви вже в цьому лобі");
        }

        int currentCount = partyMemberRepository.countByPartyRequestId(partyId);
        if (currentCount >= party.getMaxMembers()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі заповнене");
        }

        PartyMember member = PartyMember.builder()
                .partyRequest(party)
                .user(user)
                .build();
        partyMemberRepository.save(member);

        if (party.getChat() != null) {
            chatService.addParticipant(party.getChat().getId(), user);
        }

        currentCount++;
        if (currentCount >= party.getMaxMembers()) {
            party.setStatus(PartyStatus.FULL);
            party.setIsOpen(false);
            partyRequestRepository.save(party);
        }

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        achievementService.unlock(user, AchievementCatalog.FIRST_PARTY_JOINED, "AUTO");
        achievementService.syncJoinedPartyMilestones(user);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO leaveParty(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!ACTIVE_STATUSES.contains(party.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі вже неактивне");
        }

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, user.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ви не в цьому лобі");
        }

        partyMemberRepository.deleteByPartyRequestIdAndUserId(partyId, user.getId());

        if (party.getChat() != null) {
            chatService.removeParticipant(party.getChat().getId(), user);
        }

        List<PartyMember> remaining = partyMemberRepository.findByPartyRequestIdOrderByJoinedAtAsc(partyId);

        if (remaining.isEmpty()) {
            party.setStatus(PartyStatus.CANCELLED);
            party.setIsOpen(false);
            party.setCompletedAt(Instant.now());
            partyRequestRepository.save(party);
            broadcastPartyUpdate(partyId);
            return toDTO(party, remaining);
        }

        if (party.getCreator().getId().equals(user.getId())) {
            PartyMember newLeader = remaining.get(0);
            party.setCreator(newLeader.getUser());
        }

        if (party.getStatus() == PartyStatus.FULL && remaining.size() < party.getMaxMembers()) {
            party.setStatus(PartyStatus.OPEN);
            party.setIsOpen(true);
        }

        partyRequestRepository.save(party);

        PartyRequestDTO result = toDTO(party, remaining);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO kickMember(User creator, Long partyId, Long userId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!ACTIVE_STATUSES.contains(party.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі вже неактивне");
        }

        if (!party.getCreator().getId().equals(creator.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Тільки хост може кікати гравців");
        }

        if (creator.getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Не можна кікнути себе");
        }

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Гравець не в цьому лобі");
        }

        partyMemberRepository.deleteByPartyRequestIdAndUserId(partyId, userId);

        if (party.getChat() != null) {
            try {
                chatService.removeParticipantById(party.getChat().getId(), userId);
            } catch (Exception ignored) {}
        }

        try {
            voiceAccessService.kickParticipant(partyId, userId);
        } catch (Exception ignored) {}

        if (party.getStatus() == PartyStatus.FULL) {
            party.setStatus(PartyStatus.OPEN);
            party.setIsOpen(true);
            partyRequestRepository.save(party);
        }

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO closeParty(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!party.getCreator().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Тільки хост може закрити лобі");
        }

        if (!ACTIVE_STATUSES.contains(party.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі вже неактивне");
        }

        party.setStatus(PartyStatus.CANCELLED);
        party.setIsOpen(false);
        party.setCompletedAt(Instant.now());
        partyRequestRepository.save(party);

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO startGame(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!party.getCreator().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Тільки хост може почати гру");
        }

        if (party.getStatus() != PartyStatus.OPEN && party.getStatus() != PartyStatus.FULL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі має бути відкритим або заповненим для старту");
        }

        party.setStatus(PartyStatus.IN_GAME);
        party.setIsOpen(false);
        party.setStartedAt(Instant.now());
        partyRequestRepository.save(party);

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO completeParty(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!party.getCreator().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Тільки хост може завершити гру");
        }

        if (party.getStatus() != PartyStatus.IN_GAME) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі має бути в стані IN_GAME");
        }

        party.setStatus(PartyStatus.COMPLETED);
        party.setIsOpen(false);
        party.setCompletedAt(Instant.now());
        partyRequestRepository.save(party);

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        for (PartyMember member : members) {
            achievementService.unlock(member.getUser(), AchievementCatalog.FIRST_GAME_COMPLETED, "AUTO");
            achievementService.syncCompletedPartyMilestones(member.getUser());
        }
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    public List<PartyRequestDTO> getOpenParties(Long gameId, String platform,
                                                String skillLevel, String playStyle) {

        String platformParam = (platform != null && !platform.isBlank()) ? platform : null;

        List<PartyRequest> parties = partyRequestRepository.findOpenWithFilters(
                gameId, skillLevel, playStyle, platformParam
        );

        return parties.stream()
                .map(p -> {
                    int count = partyMemberRepository.countByPartyRequestId(p.getId());
                    return toListDTO(p, count);
                })
                .toList();
    }

    public PartyRequestDTO getPartyById(Long partyId) {
        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        return toDTO(party, members);
    }

    public UserStatsDTO getUserStats(Long userId) {
        long completed = partyMemberRepository.countCompletedPartiesByUserId(userId);
        long created = partyMemberRepository.countCompletedPartiesCreatedByUserId(userId);
        long joined = partyMemberRepository.countAllPartiesJoinedByUserId(userId);
        long playTimeSeconds = partyMemberRepository.sumPlayTimeSecondsByUserId(userId);
        double hoursPlayed = Math.round(playTimeSeconds / 36.0) / 100.0;

        String favGameName = null;
        String favGameImageUrl = null;
        Long favGameId = null;
        var favGameIdOpt = partyMemberRepository.findFavoriteGameIdByUserId(userId);
        if (favGameIdOpt.isPresent()) {
            var gameOpt = gameRepository.findById(favGameIdOpt.get());
            if (gameOpt.isPresent()) {
                favGameId = gameOpt.get().getId();
                favGameName = gameOpt.get().getName();
                favGameImageUrl = gameOpt.get().getImageUrl();
            }
        }

        return UserStatsDTO.builder()
                .completedGames(completed)
                .partiesCreated(created)
                .partiesJoined(joined)
                .hoursPlayed(hoursPlayed)
                .favoriteGameName(favGameName)
                .favoriteGameImageUrl(favGameImageUrl)
                .favoriteGameId(favGameId)
                .build();
    }

    public Page<PartyRequestDTO> getOpenPartiesPaged(Long gameId, String platform,
                                                     String skillLevel, String playStyle,
                                                     String language, String region,
                                                     String q, Pageable pageable) {
        return getOpenPartiesPaged(gameId, platform, skillLevel, playStyle, language, region, q, pageable, null);
    }

    public Page<PartyRequestDTO> getOpenPartiesPaged(Long gameId, String platform,
                                                     String skillLevel, String playStyle,
                                                     String language, String region,
                                                     String q, Pageable pageable, User currentUser) {

        String platformParam = (platform != null && !platform.isBlank()) ? platform : null;
        String languageParam = (language != null && !language.isBlank()) ? language : null;
        String regionParam = (region != null && !region.isBlank()) ? region : null;
        String qParam = (q != null && !q.isBlank()) ? q.trim() : null;

        Page<PartyRequest> page = partyRequestRepository.findOpenWithFiltersPaged(
                gameId, skillLevel, playStyle, platformParam, languageParam, regionParam, qParam, pageable
        );

        List<Long> blockedIds = (currentUser != null)
                ? blockService.getAllBlockedBetweenIds(currentUser.getId())
                : List.of();

        if (blockedIds.isEmpty()) {
            return page.map(p -> {
                int count = partyMemberRepository.countByPartyRequestId(p.getId());
                return toListDTO(p, count);
            });
        }

        List<PartyRequestDTO> filtered = page.getContent().stream()
                .filter(p -> !blockedIds.contains(p.getCreator().getId()))
                .map(p -> {
                    int count = partyMemberRepository.countByPartyRequestId(p.getId());
                    return toListDTO(p, count);
                })
                .toList();

        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, page.getTotalElements());
    }

    public List<PartyRequestDTO> getMyParties(User user) {
        return partyMemberRepository.findActivePartiesByUserId(user.getId()).stream()
                .map(pm -> {
                    PartyRequest party = pm.getPartyRequest();
                    int count = partyMemberRepository.countByPartyRequestId(party.getId());
                    return toListDTO(party, count);
                })
                .toList();
    }

    public Page<PartyRequestDTO> getPartyHistory(User user, Pageable pageable) {
        Page<PartyRequest> page = partyRequestRepository.findHistoryByUserId(user.getId(), pageable);
        return page.map(p -> {
            List<PartyMember> members = partyMemberRepository.findByPartyRequestId(p.getId());
            return toDTO(p, members);
        });
    }

    public Page<PartyRequestDTO> getPartyHistoryForUser(Long targetUserId, User requester, Pageable pageable) {
        if (!targetUserId.equals(requester.getId())) {
            PrivacySettings ps = privacySettingsRepository.findByUserId(targetUserId).orElse(null);
            if (ps != null) {
                VisibilityLevel vis = ps.getStatsVisibility();
                if (vis == VisibilityLevel.NOBODY) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Статистика прихована");
                }
                if (vis == VisibilityLevel.FRIENDS) {
                    boolean isFriend = friendshipRepository.areFriends(requester.getId(), targetUserId);
                    if (!isFriend) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "Статистика доступна лише друзям");
                    }
                }
            }
        }
        Page<PartyRequest> page = partyRequestRepository.findHistoryByUserId(targetUserId, pageable);
        return page.map(p -> {
            List<PartyMember> members = partyMemberRepository.findByPartyRequestId(p.getId());
            return toDTO(p, members);
        });
    }

    public List<RecentTeammateDTO> getRecentTeammates(User user) {
        List<Object[]> rows = partyMemberRepository.findRecentTeammateIds(user.getId());
        return rows.stream()
                .map(row -> {
                    Long usrId = ((Number) row[0]).longValue();
                    int count = ((Number) row[1]).intValue();
                    String displayName = userRepository.findById(usrId)
                            .map(u -> u.getDisplayName())
                            .orElse("Unknown");
                    String avatarUrl = profileRepository.findByUserId(usrId)
                            .map(p -> p.getAvatarUrl())
                            .orElse(null);
                    return RecentTeammateDTO.builder()
                            .userId(usrId)
                            .displayName(displayName)
                            .avatarUrl(avatarUrl)
                            .gamesPlayedTogether(count)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void removeUserFromActivePartiesDueToBan(Long userId) {
        List<PartyMember> memberships = partyMemberRepository.findActivePartiesByUserId(userId);
        Set<Long> processedPartyIds = new HashSet<>();

        for (PartyMember membership : memberships) {
            PartyRequest party = membership.getPartyRequest();
            Long partyId = party.getId();
            if (!processedPartyIds.add(partyId)) {
                continue;
            }

            if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, userId)) {
                continue;
            }

            partyMemberRepository.deleteByPartyRequestIdAndUserId(partyId, userId);

            if (party.getChat() != null) {
                try {
                    chatService.removeParticipantById(party.getChat().getId(), userId);
                } catch (Exception ignored) {}
            }

            try {
                voiceAccessService.kickParticipant(partyId, userId);
            } catch (Exception ignored) {}

            List<PartyMember> remaining = partyMemberRepository.findByPartyRequestIdOrderByJoinedAtAsc(partyId);

            if (remaining.isEmpty()) {
                party.setStatus(PartyStatus.CANCELLED);
                party.setIsOpen(false);
                partyRequestRepository.save(party);
                broadcastPartyUpdate(partyId);
                continue;
            }

            if (party.getCreator().getId().equals(userId)) {
                party.setCreator(remaining.get(0).getUser());
            }

            if (party.getStatus() == PartyStatus.FULL && remaining.size() < party.getMaxMembers()) {
                party.setStatus(PartyStatus.OPEN);
                party.setIsOpen(true);
            }

            partyRequestRepository.save(party);
            broadcastPartyUpdate(partyId);
        }
    }

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void autoExpireParties() {
        Instant openCutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        partyRequestRepository.cancelStaleOpenParties(openCutoff);

        Instant inGameCutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        partyRequestRepository.completeStaleInGameParties(inGameCutoff);
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void autoArchiveGameChats() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        List<PartyRequest> parties = partyRequestRepository.findPartiesWithChatsToArchive(
                List.of(PartyStatus.COMPLETED, PartyStatus.CANCELLED), cutoff);
        for (PartyRequest party : parties) {
            if (party.getChat() != null) {
                chatService.archiveChatForAllParticipants(party.getChat().getId());
            }
        }
    }

    private PartyRequest getPartyWithStatus(Long partyId, PartyStatus requiredStatus) {
        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));
        if (party.getStatus() != requiredStatus) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Лобі має бути у статусі " + requiredStatus + ", поточний: " + party.getStatus());
        }
        return party;
    }

    private PartyRequestDTO toDTO(PartyRequest party, List<PartyMember> members) {
        String creatorAvatarUrl = profileRepository.findByUserId(party.getCreator().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        List<PartyMemberDTO> memberDTOs = members.stream()
                .map(m -> {
                    String avatarUrl = profileRepository.findByUserId(m.getUser().getId())
                            .map(p -> p.getAvatarUrl())
                            .orElse(null);
                    return PartyMemberDTO.builder()
                            .userId(m.getUser().getId())
                            .displayName(m.getUser().getDisplayName())
                            .avatarUrl(avatarUrl)
                            .creator(m.getUser().getId().equals(party.getCreator().getId()))
                            .joinedAt(m.getJoinedAt())
                            .build();
                })
                .toList();

        return PartyRequestDTO.builder()
                .id(party.getId())
                .creatorId(party.getCreator().getId())
                .creatorDisplayName(party.getCreator().getDisplayName())
                .creatorAvatarUrl(creatorAvatarUrl)
                .gameId(party.getGame().getId())
                .gameName(party.getGame().getName())
                .gameImageUrl(party.getGame().getImageUrl())
                .maxMembers(party.getMaxMembers())
                .currentMembers(memberDTOs.size())
                .isOpen(party.getStatus() == PartyStatus.OPEN)
                .status(party.getStatus().name())
                .title(party.getTitle())
                .description(party.getDescription())
                .eventTime(party.getEventTime())
                .platform(party.getPlatform())
                .languages(party.getLanguages())
                .skillLevel(party.getSkillLevel() != null ? party.getSkillLevel().name() : null)
                .playStyle(party.getPlayStyle() != null ? party.getPlayStyle().name() : null)
                .tags(party.getTags())
                .region(party.getRegion() != null ? party.getRegion().name() : null)
                .members(memberDTOs)
                .chatId(party.getChat() != null ? party.getChat().getId() : null)
                .createdAt(party.getCreatedAt())
                .build();
    }

    private PartyRequestDTO toListDTO(PartyRequest party, int currentMembers) {
        String creatorAvatarUrl = profileRepository.findByUserId(party.getCreator().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        return PartyRequestDTO.builder()
                .id(party.getId())
                .creatorId(party.getCreator().getId())
                .creatorDisplayName(party.getCreator().getDisplayName())
                .creatorAvatarUrl(creatorAvatarUrl)
                .gameId(party.getGame().getId())
                .gameName(party.getGame().getName())
                .gameImageUrl(party.getGame().getImageUrl())
                .maxMembers(party.getMaxMembers())
                .currentMembers(currentMembers)
                .isOpen(party.getStatus() == PartyStatus.OPEN)
                .status(party.getStatus().name())
                .title(party.getTitle())
                .description(party.getDescription())
                .eventTime(party.getEventTime())
                .platform(party.getPlatform())
                .languages(party.getLanguages())
                .skillLevel(party.getSkillLevel() != null ? party.getSkillLevel().name() : null)
                .playStyle(party.getPlayStyle() != null ? party.getPlayStyle().name() : null)
                .tags(party.getTags())
                .region(party.getRegion() != null ? party.getRegion().name() : null)
                .members(null)
                .chatId(party.getChat() != null ? party.getChat().getId() : null)
                .createdAt(party.getCreatedAt())
                .build();
    }

    private void validatePlatforms(List<String> platforms) {
        if (platforms == null || platforms.isEmpty()) return;
        for (String p : platforms) {
            try {
                Platform.valueOf(p.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid platform: " + p + ". Allowed: " + java.util.Arrays.toString(Platform.values()));
            }
        }
    }

    private void validateLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty()) return;
        for (String lang : languages) {
            try {
                Language.valueOf(lang.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid language: " + lang + ". Allowed: " + java.util.Arrays.toString(Language.values()));
            }
        }
    }

    private void validateTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        if (tags.size() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Максимум 5 тегів");
        }
        for (String tag : tags) {
            if (tag.length() > 30) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Тег занадто довгий (макс 30 символів)");
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid " + fieldName + ": " + value);
        }
    }

    public void broadcastPartyUpdate(Long partyId) {
        try {
            PartyRequest party = partyRequestRepository.findById(partyId).orElse(null);
            if (party == null) return;
            List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
            PartyRequestDTO dto = toDTO(party, members);
            messagingTemplate.convertAndSend("/topic/party/" + partyId, dto);
        } catch (Exception ignored) {
        }
    }
}