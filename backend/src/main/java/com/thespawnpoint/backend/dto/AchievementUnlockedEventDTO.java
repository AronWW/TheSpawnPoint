package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AchievementUnlockedEventDTO {
    private String code;
    private String title;
    private String description;
    private String type;
    private String icon;
    private Instant unlockedAt;
}
