package core.domain.chat.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClovaXResponse {
    private Status status;
    private Result result;

    @Getter
    @NoArgsConstructor
    public static class Status {
        private String code;
        private String message;
    }

    @Getter
    @NoArgsConstructor
    public static class Result {
        private String stopReason;
        private Message message;
        private Integer inputLength;
        private Integer outputLength;
    }

    @Getter
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}