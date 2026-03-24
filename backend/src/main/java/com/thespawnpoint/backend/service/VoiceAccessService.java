package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.config.LiveKitProperties;
import com.thespawnpoint.backend.dto.VoiceTokenResponseDTO;
import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.party.PartyStatus;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.PartyMemberRepository;
import com.thespawnpoint.backend.repository.PartyRequestRepository;
import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanPublishSources;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class VoiceAccessService {

    private static final Set<PartyStatus> VOICE_ALLOWED_STATUSES =
            Set.of(PartyStatus.OPEN, PartyStatus.FULL, PartyStatus.IN_GAME);

    private final PartyRequestRepository partyRequestRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final LiveKitProperties liveKitProperties;

    public VoiceAccessService(
            PartyRequestRepository partyRequestRepository,
            PartyMemberRepository partyMemberRepository,
            LiveKitProperties liveKitProperties
    ) {
        this.partyRequestRepository = partyRequestRepository;
        this.partyMemberRepository = partyMemberRepository;
        this.liveKitProperties = liveKitProperties;
    }

    public VoiceTokenResponseDTO issuePartyVoiceToken(User currentUser, Long partyId) {
        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Лобі не знайдено"));

        if (!VOICE_ALLOWED_STATUSES.contains(party.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Голосовий чат недоступний для цього лобі");
        }

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ви не є учасником цього лобі");
        }

        String roomName = buildRoomName(partyId);
        String participantIdentity = buildParticipantIdentity(currentUser);
        String participantName = resolveParticipantName(currentUser);
        String token = buildParticipantToken(roomName, participantIdentity, participantName);

        return VoiceTokenResponseDTO.builder()
                .serverUrl(liveKitProperties.getUrl())
                .participantToken(token)
                .roomName(roomName)
                .participantIdentity(participantIdentity)
                .participantName(participantName)
                .build();
    }

    private String buildParticipantToken(String roomName, String participantIdentity, String participantName) {
        AccessToken accessToken = new AccessToken(
                liveKitProperties.getApiKey(),
                liveKitProperties.getApiSecret()
        );

        accessToken.setIdentity(participantIdentity);
        accessToken.setName(participantName);
        accessToken.setTtl(Duration.ofMinutes(liveKitProperties.getTokenTtlMinutes()).toMillis());
        accessToken.addGrants(
                new RoomJoin(true),
                new RoomName(roomName),
                new CanPublish(true),
                new CanPublishSources(List.of("microphone")),
                new CanSubscribe(true),
                new CanPublishData(false)
        );

        return accessToken.toJwt();
    }

    private String buildRoomName(Long partyId) {
        return "party-" + partyId;
    }

    private String buildParticipantIdentity(User currentUser) {
        return "user-" + currentUser.getId();
    }

    private String resolveParticipantName(User currentUser) {
        String displayName = currentUser.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            return buildParticipantIdentity(currentUser);
        }
        return displayName;
    }
}
