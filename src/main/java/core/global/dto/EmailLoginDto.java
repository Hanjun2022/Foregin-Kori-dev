package core.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Schema(description = "일반(이메일) 로그인 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLoginDto {

    @Schema(example = "hjun16@naver.com")
    @NotBlank
    @Email
    private String email;

    @Schema(example = "password12!")
    @NotBlank
    private String password;

}
