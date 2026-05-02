package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MessageAttachmentDTO {
    private Long id;
    private int position;
    private String mediaType;
    private String url;
    private String resourceType;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private Integer width;
    private Integer height;
    private BigDecimal durationSeconds;
}
