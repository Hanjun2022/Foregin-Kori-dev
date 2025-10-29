package core.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClovaXRequest {
    private List<Message> messages;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Double repeatPenalty;
    private Boolean stopBeforeTermination;

    @Getter
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}