package core.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LocationUpdateRequest {

    @Schema(description = "사용자의 현재 위도 (-90 ~ 90). null은 위치 정보 미동의를 의미합니다.", example = "37.5665", nullable = true)
    private Double latitude;

    @Schema(description = "사용자의 현재 경도 (-180 ~ 180). null은 위치 정보 미동의를 의미합니다.", example = "126.9780", nullable = true)
    private Double longitude;
}