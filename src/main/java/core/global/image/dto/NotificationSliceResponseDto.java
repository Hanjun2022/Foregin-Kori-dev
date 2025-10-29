package core.global.image.dto;

import core.domain.notification.dto.NotificationResponseDto;
import lombok.Builder;
import java.util.List;

/**
 * 알림 목록 조회를 위한 슬라이스 기반 응답 DTO (커서 기반 페이지네이션)
 * @param notifications 알림 데이터 목록
 * @param nextCursor 다음 페이지를 조회하기 위한 커서 (마지막 알림의 생성 시간)
 * @param hasNext 다음 페이지 존재 여부
 */
@Builder
public record NotificationSliceResponseDto(
        List<NotificationResponseDto> notifications,
        String nextCursor,
        boolean hasNext
) {}