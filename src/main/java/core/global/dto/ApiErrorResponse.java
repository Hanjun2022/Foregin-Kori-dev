package core.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String error,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public static ApiErrorResponse of(String error, String message) {
        return new ApiErrorResponse(error, message, LocalDateTime.now());
    }
}
