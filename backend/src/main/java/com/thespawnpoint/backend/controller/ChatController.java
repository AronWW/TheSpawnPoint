package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.*;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.exception.WebSocketExceptionHandler;
import com.thespawnpoint.backend.repository.UserRepository;
import com.thespawnpoint.backend.service.ChatService;
import com.thespawnpoint.backend.service.CloudinaryImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController extends WebSocketExceptionHandler {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final CloudinaryImageService cloudinaryImageService;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @GetMapping("/api/chats")
    public ResponseEntity<List<ChatDTO>> getChats(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getUserChats(currentUser));
    }

    @GetMapping("/api/chats/{partnerEmail}/messages")
    public ResponseEntity<List<MessageDTO>> history(
            @PathVariable String partnerEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getHistory(currentUser, partnerEmail, page, size));
    }

    @PostMapping("/api/chats/dm/{partnerEmail}")
    public ResponseEntity<Map<String, Long>> openDmChat(
            @PathVariable String partnerEmail,
            @AuthenticationPrincipal User currentUser) {
        User partner = userRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Long chatId = chatService.getOrCreateDmChat(currentUser, partner).getId();
        return ResponseEntity.ok(Map.of("chatId", chatId));
    }


    @PostMapping("/api/chats/group")
    public ResponseEntity<ChatDTO> createGroupChat(
            @RequestBody @jakarta.validation.Valid CreateGroupChatDTO dto,
            @AuthenticationPrincipal User currentUser) {
        ChatDTO chat = chatService.createStandaloneGroupChat(currentUser, dto.getTitle(), dto.getMemberEmails());
        return ResponseEntity.status(HttpStatus.CREATED).body(chat);
    }

    @PostMapping("/api/chats/group/{chatId}/members")
    public ResponseEntity<Void> addGroupChatMember(
            @PathVariable Long chatId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        chatService.addGroupChatParticipant(currentUser, chatId, email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/chats/group/{chatId}/leave")
    public ResponseEntity<Void> leaveGroupChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        chatService.leaveGroupChat(currentUser, chatId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/chats/group/{chatId}/title")
    public ResponseEntity<ChatDTO> renameGroupChat(
            @PathVariable Long chatId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        return ResponseEntity.ok(chatService.renameGroupChat(currentUser, chatId, title));
    }

    @GetMapping("/api/chats/group/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> groupHistory(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getGroupHistory(currentUser, chatId, page, size));
    }

    @PostMapping("/api/chats/{chatId}/archive")
    public ResponseEntity<Void> archiveChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        chatService.archiveChat(currentUser, chatId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/chats/{chatId}/archive")
    public ResponseEntity<Void> unarchiveChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        chatService.unarchiveChat(currentUser, chatId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/chats/{chatId}/pin")
    public ResponseEntity<Void> pinChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        chatService.pinChat(currentUser, chatId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/chats/{chatId}/pin")
    public ResponseEntity<Void> unpinChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        chatService.unpinChat(currentUser, chatId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/chats/{chatId}")
    public ResponseEntity<Void> deleteChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        chatService.deleteChatForUser(currentUser, chatId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/chats/{chatId}/messages/search")
    public ResponseEntity<List<MessageDTO>> searchMessages(
            @PathVariable Long chatId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.searchMessages(currentUser, chatId, q, page, size));
    }

    @GetMapping("/api/chats/{chatId}/pinned-messages")
    public ResponseEntity<List<PinnedMessageDTO>> getPinnedMessages(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getPinnedMessages(currentUser, chatId));
    }

    @GetMapping("/api/chats/messages/{messageId}/read-by")
    public ResponseEntity<List<MessageReadByDTO>> getMessageReadBy(
            @PathVariable Long messageId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getMessageReadBy(currentUser, messageId));
    }

    @DeleteMapping("/api/chats/group/{chatId}/members/{userId}")
    public ResponseEntity<Void> removeGroupMember(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        chatService.removeGroupChatMember(currentUser, chatId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/chats/group/{chatId}/members/{userId}/admin")
    public ResponseEntity<Void> grantAdmin(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        chatService.grantAdmin(currentUser, chatId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/chats/group/{chatId}/members/{userId}/admin")
    public ResponseEntity<Void> revokeAdmin(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        chatService.revokeAdmin(currentUser, chatId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/api/chats/group/{chatId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatDTO> uploadGroupAvatar(
            @PathVariable Long chatId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Файл не повинен перевищувати 2 МБ");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Тільки JPEG, PNG, WebP та GIF");
        }

        CloudinaryImageService.UploadResult result = cloudinaryImageService.uploadGroupAvatar(file, chatId);
        return ResponseEntity.ok(chatService.uploadGroupAvatar(currentUser, chatId, result.secureUrl(), result.publicId()));
    }

    @PostMapping(value = "/api/chats/{chatId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageDTO> sendChatMessage(
            @PathVariable Long chatId,
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @RequestParam(value = "replyToId", required = false) Long replyToId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal User currentUser) {
        MessageDTO message = chatService.sendChatMessage(currentUser, chatId, content, files, replyToId);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload @Valid SendMessageDTO dto, Principal principal) {
        User sender = getPrincipalUser(principal);
        chatService.sendMessage(sender, dto.getRecipientEmail(), dto.getContent(), dto.getReplyToId());
        log.debug("Message from {} to {}", sender.getEmail(), dto.getRecipientEmail());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingDTO dto, Principal principal) {
        chatService.sendTypingIndicator(getPrincipalUser(principal), dto.getRecipientEmail());
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload Map<String, String> payload, Principal principal) {
        String senderEmail = payload.get("senderEmail");
        if (senderEmail == null) return;
        chatService.markAsReadAndNotify(getPrincipalUser(principal), senderEmail);
    }

    @MessageMapping("/chat.sendGroup")
    public void sendGroupMessage(@Payload @Valid SendGroupMessageDTO dto, Principal principal) {
        User sender = getPrincipalUser(principal);
        chatService.sendGroupMessage(sender, dto.getChatId(), dto.getContent(), dto.getReplyToId());
        log.debug("Group message from {} to chat {}", sender.getEmail(), dto.getChatId());
    }

    @MessageMapping("/chat.typingGroup")
    public void typingGroup(@Payload Map<String, Object> payload, Principal principal) {
        Long chatId = ((Number) payload.get("chatId")).longValue();
        chatService.sendGroupTypingIndicator(getPrincipalUser(principal), chatId);
    }

    @MessageMapping("/chat.readGroup")
    public void markGroupRead(@Payload Map<String, Object> payload, Principal principal) {
        Long chatId = ((Number) payload.get("chatId")).longValue();
        chatService.markGroupAsRead(getPrincipalUser(principal), chatId);
    }

    @MessageMapping("/chat.readUpTo")
    public void markReadUpTo(@Payload @Valid ReadUpToDTO dto, Principal principal) {
        chatService.markChatAsReadUpTo(getPrincipalUser(principal), dto.getChatId(), dto.getMessageId());
    }

    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long messageId = ((Number) payload.get("messageId")).longValue();
        chatService.deleteMessage(getPrincipalUser(principal), messageId);
    }

    @MessageMapping("/chat.editMessage")
    public void editMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long messageId = ((Number) payload.get("messageId")).longValue();
        String newContent = (String) payload.get("newContent");
        chatService.editMessage(getPrincipalUser(principal), messageId, newContent);
    }

    @MessageMapping("/chat.toggleReaction")
    public void toggleReaction(@Payload Map<String, Object> payload, Principal principal) {
        Long messageId = ((Number) payload.get("messageId")).longValue();
        String emoji = (String) payload.get("emoji");
        chatService.toggleReaction(getPrincipalUser(principal), messageId, emoji);
    }

    @MessageMapping("/chat.pinMessage")
    public void pinMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long chatId = ((Number) payload.get("chatId")).longValue();
        Long messageId = ((Number) payload.get("messageId")).longValue();
        chatService.pinMessage(getPrincipalUser(principal), chatId, messageId);
    }

    @MessageMapping("/chat.unpinMessage")
    public void unpinMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long chatId = ((Number) payload.get("chatId")).longValue();
        Long messageId = ((Number) payload.get("messageId")).longValue();
        chatService.unpinMessage(getPrincipalUser(principal), chatId, messageId);
    }

    private User getPrincipalUser(Principal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
