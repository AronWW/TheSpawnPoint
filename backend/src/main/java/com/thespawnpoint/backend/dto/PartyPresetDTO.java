package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PartyPresetDTO {
    private Long id;
    private String name;
    private Integer slotIndex;
    private Long gameId;
    private String gameName;
    private String gameImageUrl;
    private Integer maxMembers;
    private List<String> platform;
    private List<String> languages;
    private String skillLevel;
    private String playStyle;
    private List<String> tags;
    private String region;
}

