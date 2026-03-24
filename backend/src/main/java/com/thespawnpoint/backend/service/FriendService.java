package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.FriendDTO;
import com.thespawnpoint.backend.dto.FriendRequestDTO;
import com.thespawnpoint.backend.entity.social.*;
import com.thespawnpoint.backend.entity.user.Profile;
import com.thespawnpoint.backend.entity.user.Role;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private static final int MAX_FRIENDS = 50;

    private final FriendshipRepository friendshipRepository;
    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    @Transactional
    public void sendFriendRequest(User sender, Long receiverId) {
        if (sender.getId().equals(receiverId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot send friend request to yourself");
        }

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (receiver.getRole() == com.thespawnpoint.backend.entity.user.Role.ADMIN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (friendshipRepository.areFriends(sender.getId(), receiverId)) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already friends");
        }

        var existing = inviteRepository.findPendingBetween(
                sender.getId(), receiverId, InviteType.FRIEND_REQUEST, InviteStatus.PENDING);
        if (existing.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Friend request already pending");
        }

        Invite saved = inviteRepository.save(Invite.builder()
                .sender(sender)
                .receiver(receiver)
                .type(InviteType.FRIEND_REQUEST)
                .status(InviteStatus.PENDING)
                .build());

        notificationService.send(
                receiver,
                NotificationType.FRIEND_REQUEST,
                sender.getDisplayName() + " надіслав вам запит у друзі",
                saved.getId()
        );
    }

    @Transactional
    public void acceptFriendRequest(User currentUser, Long inviteId) {
        Invite invite = getValidFriendInvite(inviteId, currentUser);

        long currentFriends = friendshipRepository.countByUserId(currentUser.getId());
        if (currentFriends >= MAX_FRIENDS) {
            throw new ApiException(HttpStatus.CONFLICT, "Ви досягли ліміту друзів (50). Видаліть когось зі списку, щоб прийняти нову заявку.");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setRespondedAt(Instant.now());
        inviteRepository.save(invite);

        User user1 = invite.getSender().getId() < invite.getReceiver().getId()
                ? invite.getSender() : invite.getReceiver();
        User user2 = invite.getSender().getId() < invite.getReceiver().getId()
                ? invite.getReceiver() : invite.getSender();

        friendshipRepository.save(Friendship.builder()
                .user1(user1)
                .user2(user2)
                .build());

        notificationService.send(
                invite.getSender(),
                NotificationType.FRIEND_REQUEST,
                currentUser.getDisplayName() + " прийняв ваш запит у друзі",
                invite.getId()
        );
    }

    @Transactional
    public void declineFriendRequest(User currentUser, Long inviteId) {
        Invite invite = getValidFriendInvite(inviteId, currentUser);

        invite.setStatus(InviteStatus.DECLINED);
        invite.setRespondedAt(Instant.now());
        inviteRepository.save(invite);
    }

    @Transactional
    public void cancelFriendRequest(User sender, Long inviteId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (!invite.getSender().getId().equals(sender.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your request");
        }

        if (invite.getType() != InviteType.FRIEND_REQUEST) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not a friend request");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request is no longer pending");
        }

        inviteRepository.delete(invite);
    }

    @Transactional
    public void unfriend(User currentUser, Long friendUserId) {
        Friendship friendship = friendshipRepository.findBetween(currentUser.getId(), friendUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Friendship not found"));

        friendshipRepository.delete(friendship);
    }

    public List<FriendDTO> getFriends(User currentUser) {
        List<Friendship> friendships = friendshipRepository.findAllByUserId(currentUser.getId());
        return toFriendDTOs(currentUser.getId(), friendships);
    }

    public List<FriendDTO> getFriendsByUserId(User currentUser, Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (target.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Friendship> friendships = friendshipRepository.findAllByUserId(targetUserId);
        return toFriendDTOs(targetUserId, friendships);
    }

    public List<FriendRequestDTO> getIncomingRequests(User currentUser) {
        return inviteRepository.findByReceiverIdAndTypeAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(), InviteType.FRIEND_REQUEST, InviteStatus.PENDING)
                .stream()
                .map(this::toFriendRequestDTO)
                .toList();
    }

    public List<FriendRequestDTO> getOutgoingRequests(User currentUser) {
        return inviteRepository.findBySenderIdAndTypeAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(), InviteType.FRIEND_REQUEST, InviteStatus.PENDING)
                .stream()
                .map(this::toFriendRequestDTO)
                .toList();
    }

    private Invite getValidFriendInvite(Long inviteId, User receiver) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (!invite.getReceiver().getId().equals(receiver.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your request");
        }

        if (invite.getType() != InviteType.FRIEND_REQUEST) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not a friend request");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request is no longer pending");
        }

        return invite;
    }

    private List<FriendDTO> toFriendDTOs(Long profileOwnerUserId, List<Friendship> friendships) {
        List<User> friends = friendships.stream()
                .map(f -> f.getUser1().getId().equals(profileOwnerUserId) ? f.getUser2() : f.getUser1())
                .toList();

        Map<Long, String> avatarByUserId = profileRepository.findAllByUserIdIn(
                        friends.stream().map(User::getId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        p -> p.getUser().getId(),
                        Profile::getAvatarUrl,
                        (a, b) -> a
                ));

        return friendships.stream()
                .map(f -> {
                    User friend = f.getUser1().getId().equals(profileOwnerUserId) ? f.getUser2() : f.getUser1();
                    return toFriendDTO(friend, f.getFriendsSince(), avatarByUserId.get(friend.getId()));
                })
                .toList();
    }

    private FriendDTO toFriendDTO(User user, Instant friendsSince, String avatarUrl) {

        return FriendDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(avatarUrl)
                .status(user.getStatus().name())
                .lastSeen(user.getLastSeen() != null ? user.getLastSeen().toString() : null)
                .friendsSince(friendsSince)
                .build();
    }

    private FriendRequestDTO toFriendRequestDTO(Invite invite) {
        String senderAvatarUrl = profileRepository.findByUserId(invite.getSender().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        String receiverAvatarUrl = profileRepository.findByUserId(invite.getReceiver().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        return FriendRequestDTO.builder()
                .inviteId(invite.getId())
                .senderId(invite.getSender().getId())
                .senderEmail(invite.getSender().getEmail())
                .senderDisplayName(invite.getSender().getDisplayName())
                .senderAvatarUrl(senderAvatarUrl)
                .receiverId(invite.getReceiver().getId())
                .receiverDisplayName(invite.getReceiver().getDisplayName())
                .receiverAvatarUrl(receiverAvatarUrl)
                .createdAt(invite.getCreatedAt())
                .build();
    }
}

