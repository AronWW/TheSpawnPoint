package com.thespawnpoint.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.thespawnpoint.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryImageService {

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.avatar-folder}")
    private String avatarFolder;

    public UploadResult uploadAvatar(MultipartFile file, Long userId) {
        try {
            String publicId = avatarFolder + "/user-" + userId + "-" + UUID.randomUUID();

            @SuppressWarnings("rawtypes")
            Map result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", true
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String uploadedPublicId = (String) result.get("public_id");

            if (secureUrl == null || uploadedPublicId == null) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary upload returned incomplete data");
            }

            return new UploadResult(secureUrl, uploadedPublicId);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar to Cloudinary");
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar to Cloudinary");
        }
    }

    public void deleteAvatar(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "invalidate", true
                    )
            );
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete old avatar from Cloudinary");
        }
    }

    public record UploadResult(String secureUrl, String publicId) {
    }
}