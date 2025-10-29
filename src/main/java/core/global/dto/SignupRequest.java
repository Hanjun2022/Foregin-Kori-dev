package core.global.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Schema(description = "회원가입 요청")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {


    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "gildongHi@example.com")
    @NotBlank @Email
    private String email;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "password12!")
    @NotBlank
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;


    @Schema(description = "약관 동의")
    private boolean agreedToTerms = false;



}
