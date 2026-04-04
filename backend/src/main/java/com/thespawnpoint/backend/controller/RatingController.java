package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.SubmitRatingsDTO;
import com.thespawnpoint.backend.dto.UserRatingDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<Void> submitRatings(
            @Valid @RequestBody SubmitRatingsDTO dto,
            @AuthenticationPrincipal User user) {
        ratingService.submitRatings(user, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserRatingDTO> getUserRating(@PathVariable Long userId) {
        return ResponseEntity.ok(ratingService.getUserRating(userId));
    }

    @GetMapping("/users")
    public ResponseEntity<Map<Long, UserRatingDTO>> getUserRatings(
            @RequestParam List<Long> ids) {
        return ResponseEntity.ok(ratingService.getUserRatingsMap(ids));
    }

    @GetMapping("/can-rate/{partyId}")
    public ResponseEntity<Map<String, Boolean>> canRate(
            @PathVariable Long partyId,
            @AuthenticationPrincipal User user) {
        boolean canRate = ratingService.canRateParty(user.getId(), partyId);
        return ResponseEntity.ok(Map.of("canRate", canRate));
    }
}

