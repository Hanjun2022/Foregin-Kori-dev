package core.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class MessageSliceResponse {
    private List<AiMessageResponse> content;
    private boolean hasNext;
}