package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateFeaturedAchievementsDTO {
    @NotNull
    @Size(max = 4)
    private List<String> codes;
}
