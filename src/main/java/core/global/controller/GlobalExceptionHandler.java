package core.global.controller;



import core.global.dto.ApiErrorResponse;
import core.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorResponse.of(
                        ex.getStatus().name(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handle405(HttpRequestMethodNotSupportedException ex,
                                                      HttpServletRequest req) {
        Set<HttpMethod> supported = ex.getSupportedHttpMethods();

        log.warn("405 Method Not Allowed | method={} uri={} query={} supported={} ua=\"{}\" remote={}",
                req.getMethod(),
                req.getRequestURI(),
                req.getQueryString(),
                supported,
                req.getHeader("User-Agent"),
                req.getRemoteAddr()
        );

        HttpHeaders headers = new HttpHeaders();
        if (supported != null && !supported.isEmpty()) {
            headers.setAllow(supported); // Allow: GET,POST,...
        }

        return new ResponseEntity<>(
                ApiErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED.name(), "지원하지 않는 HTTP 메서드입니다."),
                headers,
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest req) {
        log.error("Unexpected Error Occurred: method={} uri={} query={} ua=\"{}\" remote={} msg={}",
                req.getMethod(), req.getRequestURI(), req.getQueryString(),
                req.getHeader("User-Agent"), req.getRemoteAddr(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.name(),
                        "알 수 없는 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                ));
    }
}