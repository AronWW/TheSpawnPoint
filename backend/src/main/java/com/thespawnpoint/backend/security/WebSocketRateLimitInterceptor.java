package com.thespawnpoint.backend.security;

import com.thespawnpoint.backend.repository.UserRepository;
import com.thespawnpoint.backend.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private final RateLimitingService rateLimitingService;
    private final UserRepository userRepository;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;


    private static final Set<String> MESSAGE_DESTINATIONS = Set.of(
            "/app/chat.send",
            "/app/chat.sendGroup"
    );

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.SEND.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null || !MESSAGE_DESTINATIONS.contains(destination)) {
            return message;
        }

        Principal principal = accessor.getUser();
        if (principal == null) {
            return message;
        }

        String email = principal.getName();
        Optional<com.thespawnpoint.backend.entity.user.User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return message;
        }

        com.thespawnpoint.backend.entity.user.User user = userOpt.get();
        Bucket bucket = rateLimitingService.resolveBucket(user.getId().toString(), "CHAT");

        if (!bucket.tryConsume(1)) {
            log.debug("Rate limit exceeded for user {} on {}", email, destination);

            SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSendToUser(
                        email,
                        "/queue/errors",
                        Map.of(
                                "error", "RATE_LIMITED",
                                "message", "Не надсилайте повідомлення так швидко! Зачекайте кілька секунд."
                        )
                );
            }

            return null;
        }

        return message;
    }
}