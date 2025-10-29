package core.global.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AppleUserInfo {
    private String sub;             // 고유 사용자 ID
    private String email;           // 이메일 (첫 로그인 시만 제공 가능)
    private Boolean emailVerified;
}
