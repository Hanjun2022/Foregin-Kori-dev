package core.domain.chat.dto;

import java.util.List;

public record ChatParticipantAddRequest(List<Long> userIds) {}