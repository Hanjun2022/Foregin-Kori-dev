package core.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "PostUpdateRequest", description = "게시글 수정 요청")
public record PostUpdateRequest(

        @Schema(description = "본문 내용(선택 입력)", example = "본문을 이렇게 수정합니다.", maxLength = 5000)
        @Size(max = 5000, message = "content는 최대 5,000자까지 허용됩니다.")
        String content,

        @Schema(
                description = "추가할 이미지 URL 목록(선택 입력)",
                example = "[\"https://cdn.example.com/img/1.png\", \"https://cdn.example.com/img/2.jpg\"]"
        )
        @Size(max = 5, message = "images는 최대 5개까지 허용됩니다.")
        List<
                @Size(max = 2048, message = "이미지 URL은 최대 2,048자까지 허용됩니다.")
                        String
                > images,

        @Schema(
                description = "삭제할 기존 이미지 URL 목록(선택 입력)",
                example = "[\"https://cdn.example.com/img/old1.png\"]"
        )
        @Size(max = 5, message = "removedImages는 최대 5개까지 허용됩니다.")
        List<
                @Size(max = 2048, message = "이미지 URL은 최대 2,048자까지 허용됩니다.")
                        String
                > removedImages
) { }
