package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileDTO {

    @Size(min = 2, max = 30, message = "Display name must be between 2 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\- ]+$", message = "Display name can only contain letters, numbers, spaces, hyphens and underscores")
    private String displayName;

    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    private LocalDate birthDate;

    private List<String> platforms;

    private String skillLevel;

    private String playStyle;

    private List<String> languages;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    private String region;

    @Size(max = 100, message = "Discord must not exceed 100 characters")
    @Pattern(
            regexp = "^$|^(?=.{2,37}$)(?!.*\\.\\.)(?!\\.)(?!.*\\.$)[A-Za-z0-9._]+(?:#[0-9]{4})?$",
            message = "Discord must be a valid username (not a URL)"
    )
    private String discord;

    @Size(max = 200, message = "Steam URL must not exceed 200 characters")
    @Pattern(
            regexp = "^$|^(?:https?://(?:www\\.)?steamcommunity\\.com/(?:id/[A-Za-z0-9_-]{2,32}|profiles/\\d{17})/?(?:\\?.*)?|\\d{17}|[A-Za-z0-9_-]{2,32})$",
            message = "Steam must be a valid steamcommunity URL, vanity id, or steamid64"
    )
    private String steam;

    @Size(max = 200, message = "Twitch URL must not exceed 200 characters")
    @Pattern(
            regexp = "^$|^(?:https?://(?:www\\.)?twitch\\.tv/[A-Za-z0-9_]{4,25}/?|[A-Za-z0-9_]{4,25})$",
            message = "Twitch must be a valid twitch.tv URL or login"
    )
    private String twitch;

    @Size(max = 200, message = "Xbox gamertag must not exceed 200 characters")
    @Pattern(
            regexp = "^$|^(?:https?://account\\.xbox\\.com(?:/[A-Za-z]{2}-[A-Za-z]{2})?/profile\\?gamertag=[^\\s&#?]{3,15}|(?=.{3,15}$)[A-Za-z0-9][A-Za-z0-9 ]{2,14})$",
            message = "Xbox must be a valid gamertag or account.xbox.com profile URL"
    )
    private String xbox;

    @Size(max = 200, message = "PlayStation ID must not exceed 200 characters")
    @Pattern(
            regexp = "^$|^(?:https?://(?:www\\.)?psnprofiles\\.com/[A-Za-z0-9_.-]{3,16}/?|https?://my\\.playstation\\.com/profile/[A-Za-z0-9_.-]{3,16}/?|[A-Za-z0-9_.-]{3,16})$",
            message = "PlayStation must be a valid PSN ID or official profile URL"
    )
    private String playstation;

    @Size(max = 200, message = "Nintendo friend code must not exceed 200 characters")
    @Pattern(
            regexp = "^$|^(?:SW-)?\\d{4}-\\d{4}-\\d{4}$",
            message = "Nintendo friend code must match SW-1234-5678-9012"
    )
    private String nintendo;

    @Size(max = 20, message = "Banner URL must not exceed 20 characters")
    private String bannerUrl;
}