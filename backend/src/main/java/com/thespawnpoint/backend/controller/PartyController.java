package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.CreatePartyRequestDTO;
import com.thespawnpoint.backend.dto.PartyInviteDTO;
import com.thespawnpoint.backend.dto.PartyRequestDTO;
import com.thespawnpoint.backend.dto.RecentTeammateDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.PartyInviteService;
import com.thespawnpoint.backend.service.PartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;
    private final PartyInviteService partyInviteService;

    @PostMapping
    public ResponseEntity<PartyRequestDTO> createParty(
            @Valid @RequestBody CreatePartyRequestDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.createParty(user, dto));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<PartyRequestDTO> joinParty(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.joinParty(user, id));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<PartyRequestDTO> leaveParty(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.leaveParty(user, id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<PartyRequestDTO> closeParty(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.closeParty(user, id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<PartyRequestDTO> startGame(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.startGame(user, id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<PartyRequestDTO> completeParty(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.completeParty(user, id));
    }

    @PostMapping("/{partyId}/kick/{userId}")
    public ResponseEntity<PartyRequestDTO> kickMember(
            @PathVariable Long partyId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User creator) {
        return ResponseEntity.ok(partyService.kickMember(creator, partyId, userId));
    }

    @GetMapping
    public ResponseEntity<Page<PartyRequestDTO>> getOpenParties(
            @RequestParam(required = false) Long gameId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String skillLevel,
            @RequestParam(required = false) String playStyle,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.getOpenPartiesPaged(
                gameId, platform, skillLevel, playStyle, language, region, null,
                PageRequest.of(page, size), user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyRequestDTO> getParty(@PathVariable Long id) {
        return ResponseEntity.ok(partyService.getPartyById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PartyRequestDTO>> searchParties(
            @RequestParam(required = false) Long gameId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String skillLevel,
            @RequestParam(required = false) String playStyle,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.getOpenPartiesPaged(
                gameId, platform, skillLevel, playStyle, language, region, q,
                PageRequest.of(page, size), user));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PartyRequestDTO>> getMyParties(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.getMyParties(user));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<PartyRequestDTO>> getPartyHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(partyService.getPartyHistory(user, PageRequest.of(page, size)));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<Page<PartyRequestDTO>> getPartyHistoryForUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal User requester,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(partyService.getPartyHistoryForUser(userId, requester, PageRequest.of(page, size)));
    }

    @GetMapping("/recent-teammates")
    public ResponseEntity<List<RecentTeammateDTO>> getRecentTeammates(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.getRecentTeammates(user));
    }

    @PostMapping("/{partyId}/invite/{userId}")
    public ResponseEntity<PartyInviteDTO> sendPartyInvite(
            @PathVariable Long partyId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User sender) {
        return ResponseEntity.ok(partyInviteService.sendPartyInvite(sender, partyId, userId));
    }

    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<Void> acceptPartyInvite(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal User user) {
        partyInviteService.acceptPartyInvite(user, inviteId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<Void> declinePartyInvite(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal User user) {
        partyInviteService.declinePartyInvite(user, inviteId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/invites/{inviteId}")
    public ResponseEntity<Void> cancelPartyInvite(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal User sender) {
        partyInviteService.cancelPartyInvite(sender, inviteId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/invites/incoming")
    public ResponseEntity<List<PartyInviteDTO>> getIncomingPartyInvites(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyInviteService.getIncomingPartyInvites(user));
    }

    @GetMapping("/invites/outgoing")
    public ResponseEntity<List<PartyInviteDTO>> getOutgoingPartyInvites(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyInviteService.getOutgoingPartyInvites(user));
    }

    @GetMapping("/{partyId}/invites")
    public ResponseEntity<List<PartyInviteDTO>> getPartyInvites(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(partyInviteService.getPartyInvites(partyId));
    }
}

