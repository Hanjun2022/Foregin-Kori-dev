package core.global.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 이메일 인증용
 */

@Schema(description = "이메일 인증용")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String email;
    private String lang; // "ko", "en" 같은 ISO 코드
}
