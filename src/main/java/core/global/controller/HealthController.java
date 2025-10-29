package core.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Operation(summary = "health check", description = "상태 체크합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500", description = "실행 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\n  \"code\": \"W001\"," +
                                                              "\n  \"message\": \"\"}"))
            )
    })
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
