package core.global.image.dto;

import core.global.enums.ImageType;

import java.util.List;

public record PresignedUrlRequest(
        ImageType imageType,
        String uploadSessionId,
        List<FileSpec> files
) {
    public record FileSpec(
            String filename,
            String contentType
    ) {}
}
