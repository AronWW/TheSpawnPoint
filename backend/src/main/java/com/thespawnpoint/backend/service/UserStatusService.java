package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.entity.user.VisibilityLevel;
import com.thespawnpoint.backend.repository.PrivacySettingsRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserRepository userRepository;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void setOnline(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setStatus(User.Status.ONLINE);
            userRepository.save(user);
            broadcastStatus(user, User.Status.ONLINE, null);
            broadcastOnlineCount();
            log.debug("User online: {}", email);
        });
    }

    @Transactional
    public void setOffline(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            Instant now = Instant.now();
            user.setStatus(User.Status.OFFLINE);
            user.setLastSeen(now);
            userRepository.save(user);
            broadcastStatus(user, User.Status.OFFLINE, now.toString());
            broadcastOnlineCount();
            log.debug("User offline: {}", email);
        });
    }

    private void broadcastStatus(User user, User.Status status, String lastSeen) {
        String statusVisibility = privacySettingsRepository.findByUserId(user.getId())
                .map(ps -> ps.getStatusVisibility().name())
                .orElse(VisibilityLevel.ALL.name());

        Object payload = Map.of(
                "email", user.getEmail(),
                "status", status.name(),
                "lastSeen", lastSeen != null ? lastSeen : "",
                "statusVisibility", statusVisibility
        );
        messagingTemplate.convertAndSend("/topic/status", payload);
    }

    private void broadcastOnlineCount() {
        long count = userRepository.countByStatus(User.Status.ONLINE);
        Object payload = Map.of("count", count);
        messagingTemplate.convertAndSend("/topic/online-count", payload);
    }
}

