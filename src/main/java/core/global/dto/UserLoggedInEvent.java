package core.global.dto;

public record UserLoggedInEvent(
        String userId,
        String device
) {}