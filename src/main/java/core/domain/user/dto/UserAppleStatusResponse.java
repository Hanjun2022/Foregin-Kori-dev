package core.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserAppleStatusResponse(
        @Schema(description = "애플 소셜 로그인 유저 여부", example = "true")
        boolean isAppleUser,

        @Schema(description = "이름 정보가 없는 재가입 유저 여부 (애플 유저일 경우에만 의미 있음)", example = "false")
        boolean isRejoiningWithoutFullName
) {
}