package core.domain.notification.dto;


import core.domain.user.entity.User;
import lombok.Builder;

@Builder
public record ActorDto(
        Long id,
        String name,
        String profileImageUrl
) {}