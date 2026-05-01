package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadUpToDTO {
    @NotNull
    private Long chatId;

    @NotNull
    private Long messageId;
}
