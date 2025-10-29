package core.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequest {
    private String email;
    /**
     * 이메일 인증번호 6자리
     */
    private String verificationCode;
}
