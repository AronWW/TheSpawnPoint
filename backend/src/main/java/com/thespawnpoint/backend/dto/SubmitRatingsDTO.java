package com.thespawnpoint.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitRatingsDTO {

    @NotNull(message = "partyId is required")
    private Long partyId;

    @NotNull(message = "ratings are required")
    @Valid
    private List<IndividualRating> ratings;

    @Data
    public static class IndividualRating {

        @NotNull(message = "userId is required")
        private Long userId;

        @NotNull(message = "score is required")
        @Min(value = 1, message = "Score must be between 1 and 5")
        @Max(value = 5, message = "Score must be between 1 and 5")
        private Integer score;
    }
}

