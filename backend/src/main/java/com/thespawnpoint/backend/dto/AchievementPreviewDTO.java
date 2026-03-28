package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AchievementPreviewDTO {
    private int totalCount;
    private int unlockedCount;
    private List<AchievementDTO> items;
}
