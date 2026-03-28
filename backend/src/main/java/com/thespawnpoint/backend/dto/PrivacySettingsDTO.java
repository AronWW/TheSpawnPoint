package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrivacySettingsDTO {
    private String friendsVisibility;
    private String statusVisibility;
    private String favoriteGamesVisibility;
    private String statsVisibility;
    private String socialsVisibility;
    private String commentsPolicy;
    private String achievementsVisibility;
}

