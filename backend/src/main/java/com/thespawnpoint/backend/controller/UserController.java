package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.UserSummaryDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.repository.UserRepository;
import com.thespawnpoint.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<List<UserSummaryDTO>> search(
            @RequestParam String q,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(userService.search(q, currentUser));
    }

    @GetMapping("/online-count")
    public ResponseEntity<Map<String, Long>> getOnlineCount() {
        long count = userRepository.countByStatus(User.Status.ONLINE);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
