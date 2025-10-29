package core.domain.user.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import core.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "사용자 프로필 설정 요청 DTO")
public record UserProfileEditDto(

        @Schema(description = "이름 (first name)", example = "John")
        String firstname,

        @Schema(description = "성 (last name)", example = "Doe")
        String lastname,

        @Schema(description = "성별", example = "Male")
        String gender,

        @Schema(description = "생년월일 (yyyy-MM-dd 형식)", example = "1990-05-12")
        String birthday,

        @Schema(description = "국가 코드 (ISO 3166-1 alpha-2 형식)", example = "KR")
        String country,

        @Schema(description = "자기소개 (최대 40자)", example = "열정적인 개발자입니다.")
        String introduction,

        @Schema(description = "사용 목적 (최대 40자)", example = "언어 학습")
        String purpose,

        @Schema(description = "사용 가능한 언어 목록", example = "[\"english(en)\", \"korean(ko)\"]")
        List<String> language,

        @Schema(description = "취미 목록", example = "[\"reading\", \"traveling\"]")
        List<String> hobby,

        @Schema(description = "프로필 이미지 키", example = "profile/john_doe_123.jpg")
        String imageKey
) {
    public UserProfileEditDto(User user, List<String> languages, List<String> hobbies, String imageKey) {
        this(
                user.getFirstName(),
                user.getLastName(),
                user.getSex(),
                user.getBirthdate(),
                user.getCountry(),
                user.getIntroduction(),
                user.getPurpose(),
                languages,
                hobbies,
                imageKey
        );
    }
}
