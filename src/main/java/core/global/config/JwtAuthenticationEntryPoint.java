package core.global.config;

// import 생략

import com.fasterxml.jackson.databind.ObjectMapper;
import core.global.dto.ApiErrorResponse;
import core.global.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String errorMessage = authException.getMessage();
        int httpStatus = HttpStatus.UNAUTHORIZED.value();

        if (ErrorCode.JWT_TOKEN_NOT_FOUND.getMessage().equals(errorMessage) ||
                ErrorCode.JWT_TOKEN_INVALID.getMessage().equals(errorMessage)) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
        } else if (ErrorCode.JWT_TOKEN_EXPIRED.getMessage().equals(errorMessage) ||
                ErrorCode.JWT_TOKEN_BLACKLISTED.getMessage().equals(errorMessage)) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
        }

        // 응답 객체 생성
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                errorMessage,
                String.valueOf(httpStatus),
                LocalDateTime.now()
        );

        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(errorResponse));
        writer.flush();
    }
}