package com.thespawnpoint.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.thespawnpoint.backend.entity.chat.MessageAttachmentType;
import com.thespawnpoint.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryMediaService {

    private final Cloudinary cloudinary;

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> GIF_TYPES = Set.of("image/gif");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav",
            "audio/ogg", "audio/webm", "audio/mp4", "audio/aac"
    );
    private static final Set<String> TEXT_TYPES = Set.of(
            "text/plain", "text/markdown", "text/csv", "application/json", "application/xml", "text/xml"
    );
    private static final Set<String> PDF_TYPES = Set.of("application/pdf");

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_GIF_SIZE = 20L * 1024 * 1024;
    private static final long MAX_AUDIO_SIZE = 30L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100L * 1024 * 1024;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    @Value("${app.cloudinary.media-folder:thespawnpoint/chat-media}")
    private String mediaFolder;

    public ClassifiedFile classify(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String contentType = normalizeContentType(file.getContentType());
        String filename = file.getOriginalFilename();

        MessageAttachmentType mediaType;
        String resourceType;
        long maxSize;

        if (GIF_TYPES.contains(contentType)) {
            mediaType = MessageAttachmentType.GIF;
            resourceType = "image";
            maxSize = MAX_GIF_SIZE;
        } else if (IMAGE_TYPES.contains(contentType)) {
            mediaType = MessageAttachmentType.IMAGE;
            resourceType = "image";
            maxSize = MAX_IMAGE_SIZE;
        } else if (VIDEO_TYPES.contains(contentType)) {
            mediaType = MessageAttachmentType.VIDEO;
            resourceType = "video";
            maxSize = MAX_VIDEO_SIZE;
        } else if (AUDIO_TYPES.contains(contentType)) {
            mediaType = MessageAttachmentType.AUDIO;
            resourceType = "video";
            maxSize = MAX_AUDIO_SIZE;
        } else if (TEXT_TYPES.contains(contentType) || looksLikeTextFile(filename)) {
            mediaType = MessageAttachmentType.TEXT_FILE;
            resourceType = "raw";
            maxSize = MAX_FILE_SIZE;
        } else if (PDF_TYPES.contains(contentType) || looksLikePdfFile(filename)) {
            mediaType = MessageAttachmentType.PDF;
            resourceType = "raw";
            maxSize = MAX_FILE_SIZE;
        } else {
            mediaType = MessageAttachmentType.FILE;
            resourceType = "raw";
            maxSize = MAX_FILE_SIZE;
        }

        if (file.getSize() > maxSize) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is too large for type " + mediaType.name());
        }

        return new ClassifiedFile(mediaType, resourceType, contentType, maxSize);
    }

    public UploadResult uploadChatAttachment(MultipartFile file, Long chatId, Long messageId, int position, ClassifiedFile classifiedFile) {
        String publicId = mediaFolder + "/chat-" + chatId + "/message-" + messageId + "-" + position + "-" + UUID.randomUUID();
        if ("raw".equals(classifiedFile.resourceType())) {
            publicId += safeExtension(file.getOriginalFilename());
        }

        try {
            @SuppressWarnings("rawtypes")
            Map result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", classifiedFile.resourceType(),
                            "overwrite", true
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String uploadedPublicId = (String) result.get("public_id");

            if (secureUrl == null || uploadedPublicId == null) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary upload returned incomplete data");
            }

            return new UploadResult(
                    secureUrl,
                    uploadedPublicId,
                    classifiedFile.resourceType(),
                    asInteger(result.get("width")),
                    asInteger(result.get("height")),
                    asBigDecimal(result.get("duration"))
            );
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to Cloudinary");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to Cloudinary");
        }
    }

    public void deleteMedia(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", resourceType == null || resourceType.isBlank() ? "image" : resourceType,
                            "invalidate", true
                    )
            );
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete media from Cloudinary");
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.toLowerCase(Locale.ROOT);
    }

    private boolean looksLikeTextFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt")
                || lower.endsWith(".md")
                || lower.endsWith(".log")
                || lower.endsWith(".csv")
                || lower.endsWith(".json")
                || lower.endsWith(".xml");
    }

    private boolean looksLikePdfFile(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private String safeExtension(String filename) {
        if (filename == null) return ".bin";
        String clean = filename.replace("\\", "/");
        int slash = clean.lastIndexOf('/');
        if (slash >= 0) clean = clean.substring(slash + 1);
        int dot = clean.lastIndexOf('.');
        if (dot < 0 || dot == clean.length() - 1) return ".bin";
        String ext = clean.substring(dot).toLowerCase(Locale.ROOT);
        if (ext.length() > 12 || !ext.matches("\\.[a-z0-9]+")) return ".bin";
        return ext;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    public record ClassifiedFile(
            MessageAttachmentType mediaType,
            String resourceType,
            String contentType,
            long maxSize
    ) {
    }

    public record UploadResult(
            String secureUrl,
            String publicId,
            String resourceType,
            Integer width,
            Integer height,
            BigDecimal durationSeconds
    ) {
    }
}
