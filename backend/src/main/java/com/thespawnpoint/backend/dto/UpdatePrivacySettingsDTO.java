package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdatePrivacySettingsDTO {

    @NotNull
    @Pattern(regexp = "ALL|FRIENDS|NOBODY", message = "Must be ALL, FRIENDS or NOBODY")
    private String friendsVisibility;

    @NotNull
    @Pattern(regexp = "ALL|FRIENDS|NOBODY", message = "Must be ALL, FRIENDS or NOBODY")
    private String statusVisibility;

    @NotNull
    @Pattern(regexp = "ALL|FRIENDS|NOBODY", message = "Must be ALL, FRIENDS or NOBODY")
    private String favoriteGamesVisibility;

    @NotNull
    @Pattern(regexp = "ALL|FRIENDS|NOBODY", message = "Must be ALL, FRIENDS or NOBODY")
    private String statsVisibility;

    @NotNull
    @Pattern(regexp = "ALL|FRIENDS|NOBODY", message = "Must be ALL, FRIENDS or NOBODY")
    private String socialsVisibility;

    @NotNull
    @Pattern(regexp = "ALL|FRIENDS|NOBODY", message = "Must be ALL, FRIENDS or NOBODY")
    private String commentsPolicy;
}

