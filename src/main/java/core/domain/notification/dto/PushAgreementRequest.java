package core.domain.notification.dto;

public record PushAgreementRequest(
        boolean osPermissionGranted
) {}