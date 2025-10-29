package core.global.image.dto;

import java.util.Map;

public record PresignedUrlResponse(
        String key,
        String putUrl,
        String method,
        Map<String, String> headers
) {}