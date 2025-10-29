package core.global.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Apple 로그인 요청을 받을 Record DTO
 * record는 불변 데이터 객체를 간결하게 생성하는 데 사용됩니다.
 */
public record AppleLoginByCodeRequest(
        /**
         * Apple이 발급한 JWT. 사용자 신원 확인의 핵심. (필수)
         */
        @NotBlank(message = "Identity Token은 필수입니다.")
        String identityToken,

        /**
         * Access/Refresh 토큰 발급용 일회성 코드. (선택)
         */
        String authorizationCode,

        /**
         * 재전송 공격 방지를 위한 임시 값. (보안을 위해 필수)
         */
        @NotBlank(message = "Nonce 값은 필수입니다.") // Nonce는 보안상 필수로 받는 것이 좋습니다.
        String nonce,

        /**
         * 사용자 이메일. 최초 로그인 시에만 값이 존재할 수 있음. (선택)
         */
        String email,

        /**
         * 사용자 이름. 최초 로그인 시에만 값이 존재할 수 있음. (선택)
         */
        FullNameDto fullName
) {
    /**
     * 사용자 이름을 담는 중첩 Record
     */
    public record FullNameDto(
            String givenName, // 이름
            String familyName  // 성
    ) {}
}
