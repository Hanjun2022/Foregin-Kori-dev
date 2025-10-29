package core.global.exception;

import core.global.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getErrorCode();
        this.errorCode = errorCode;
    }

    public BusinessException(HttpStatus status, ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.errorCode = null;
    }
}
