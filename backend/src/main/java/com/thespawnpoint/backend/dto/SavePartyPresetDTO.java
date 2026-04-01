package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SavePartyPresetDTO {

    @NotBlank(message = "Назва шаблону обов'язкова")
    @Size(max = 50, message = "Назва не більше 50 символів")
    private String name;

    @NotNull(message = "Слот обов'язковий")
    @Min(0)
    @Max(9)
    private Integer slotIndex;

    @NotNull(message = "Гра обов'язкова")
    private Long gameId;

    private Integer maxMembers;

    private List<String> platform;

    private List<String> languages;

    private String skillLevel;

    private String playStyle;

    private List<String> tags;

    private String region;
}

