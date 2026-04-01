package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.BlockedUserDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/{userId}")
    public ResponseEntity<Void> blockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        blockService.blockUser(currentUser, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unblockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        blockService.unblockUser(currentUser, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<BlockedUserDTO>> getBlockedUsers(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blockService.getBlockedUsers(currentUser));
    }
}

