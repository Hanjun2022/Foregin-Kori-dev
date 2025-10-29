package core.global.dto;


/** 컨트롤러에서 JSON 응답으로 내려줄 DTO */
public record ResetToken(String value, long expiresInSeconds) {

}