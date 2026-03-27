package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.UserSummaryDTO;
import com.thespawnpoint.backend.entity.user.PrivacySettings;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.entity.user.VisibilityLevel;
import com.thespawnpoint.backend.repository.FriendshipRepository;
import com.thespawnpoint.backend.repository.PrivacySettingsRepository;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final FriendshipRepository friendshipRepository;

    public List<UserSummaryDTO> search(String query, User currentUser) {
        return userRepository.searchByQuery(query, currentUser.getId())
                .stream()
                .map(u -> toSearchDTO(u, currentUser))
                .toList();
    }

    private UserSummaryDTO toSearchDTO(User u, User viewer) {
        String avatarUrl = profileRepository.findByUserId(u.getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        String status = u.getStatus().name();
        String lastSeen = u.getLastSeen() != null ? u.getLastSeen().toString() : null;

        PrivacySettings ps = privacySettingsRepository.findByUserId(u.getId()).orElse(null);
        if (ps != null && !u.getId().equals(viewer.getId())) {
            boolean isFriend = friendshipRepository.areFriends(viewer.getId(), u.getId());
            boolean visible = switch (ps.getStatusVisibility()) {
                case ALL -> true;
                case FRIENDS -> isFriend;
                case NOBODY -> false;
            };
            if (!visible) {
                status = "OFFLINE";
                lastSeen = null;
            }
        }

        return UserSummaryDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .displayName(u.getDisplayName())
                .avatarUrl(avatarUrl)
                .status(status)
                .lastSeen(lastSeen)
                .lastMessage(null)
                .lastMessageAt(null)
                .unreadCount(0)
                .build();
    }
}
