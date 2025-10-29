package core.global.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Schema(description = "이메일 중복 확인 용 dto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCheckRequest {

    @Schema(example = "kildong@example.com")
    @NotBlank
    @Email
    private String email;

}
