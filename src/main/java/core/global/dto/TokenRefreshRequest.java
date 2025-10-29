package core.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenRefreshRequest(@JsonProperty("refreshToken") String refreshToken) {
}