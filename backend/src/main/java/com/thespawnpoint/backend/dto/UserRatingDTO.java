package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRatingDTO {
    private Double rating;
    private Integer ratingCount;
    private Boolean ratingVisible;
}

