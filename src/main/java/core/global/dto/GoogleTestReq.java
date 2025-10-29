package core.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 예시: GoogleLoginReq.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTestReq {
    @Schema(description = "구글로부터 발급받은 Access Token", required = true, example = "ya29.a0AS3H6NzTIo2qp...")
    private String accessToken;
}