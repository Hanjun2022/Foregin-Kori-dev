package core.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SuggestClickRequest(
        @NotBlank
        @Schema(description = "본문", example = "첫 글입니다! 반가워요 :)")
        String text
) {
}
