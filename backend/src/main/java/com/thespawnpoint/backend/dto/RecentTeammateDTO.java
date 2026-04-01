package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentTeammateDTO {
    private Long userId;
    private String displayName;
    private String avatarUrl;
    private int gamesPlayedTogether;
}

