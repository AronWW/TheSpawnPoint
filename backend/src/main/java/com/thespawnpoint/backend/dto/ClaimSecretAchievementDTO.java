package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimSecretAchievementDTO {
    @NotBlank
    private String code;
}
