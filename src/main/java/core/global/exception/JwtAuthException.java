package core.global.exception;

public class JwtAuthException extends RuntimeException {
    public JwtAuthException(String message) {
        super(message);
    }
}