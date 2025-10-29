package core.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


@Schema(description = "게시글 작성 요청")
public record PostWriteRequest(

        @Schema(description = "본문", example = "첫 글입니다! 반가워요 :)")
        @NotBlank(message = "내용은 필수입니다.")
        @Size(min = 1, max = 10_000, message = "내용은 최대 10,000자까지 가능합니다.")
        String content,

        @Schema(description = "익명 작성 여부", example = "true")
        @NotNull(message = "익명 여부는 필수입니다.")
        Boolean isAnonymous,

        @Schema(description = "이미지 URL 목록 (최대 5개)",
                example = "[\"https://cdn.example.com/p/1.png\",\"https://cdn.example.com/p/2.jpg\"]")
        @Size(max = 5, message = "이미지는 최대 5개까지 첨부 가능합니다.")
        List<
                @NotBlank(message = "이미지 URL이 비어있습니다.")
                        String
                > imageUrls

) { }