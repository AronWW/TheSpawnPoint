package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class
AchievementDTO {
    private String code;
    private String title;
    private String description;
    private String type;
    private String icon;
    private String requirementText;
    private String secretHint;
    private boolean hiddenBeforeUnlock;
    private boolean unlocked;
    private Instant unlockedAt;
    private int order;
    private Integer currentProgress;
    private Integer targetProgress;
    private Integer progressPercent;
    private boolean showProgress;
}
