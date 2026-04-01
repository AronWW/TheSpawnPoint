package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.BlockedUserDTO;
import com.thespawnpoint.backend.entity.social.*;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final InviteRepository inviteRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public void blockUser(User blocker, Long blockedId) {
        if (blocker.getId().equals(blockedId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot block yourself");
        }

        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blockedId)) {
            throw new ApiException(HttpStatus.CONFLICT, "User is already blocked");
        }

        userBlockRepository.save(UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build());

        friendshipRepository.findBetween(blocker.getId(), blockedId)
                .ifPresent(friendshipRepository::delete);

        inviteRepository.findPendingBetween(
                blocker.getId(), blockedId, InviteType.FRIEND_REQUEST, InviteStatus.PENDING
        ).ifPresent(invite -> {
            invite.setStatus(InviteStatus.DECLINED);
            invite.setRespondedAt(java.time.Instant.now());
            inviteRepository.save(invite);
        });
    }

    @Transactional
    public void unblockUser(User blocker, Long blockedId) {
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blockedId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User is not blocked");
        }
        userBlockRepository.deleteByBlockerIdAndBlockedId(blocker.getId(), blockedId);
    }

    @Transactional(readOnly = true)
    public List<BlockedUserDTO> getBlockedUsers(User user) {
        return userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(block -> {
                    String avatarUrl = profileRepository.findByUserId(block.getBlocked().getId())
                            .map(p -> p.getAvatarUrl())
                            .orElse(null);
                    return BlockedUserDTO.builder()
                            .userId(block.getBlocked().getId())
                            .displayName(block.getBlocked().getDisplayName())
                            .avatarUrl(avatarUrl)
                            .createdAt(block.getCreatedAt())
                            .build();
                })
                .toList();
    }

    public boolean isBlockedBetween(Long id1, Long id2) {
        return userBlockRepository.isBlockedBetween(id1, id2);
    }

    public List<Long> getAllBlockedBetweenIds(Long userId) {
        return userBlockRepository.findAllBlockedBetweenIds(userId);
    }
}


