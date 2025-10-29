package core.domain.user.dto;

public class UserWithdrawalEvent {
    private final Long userId;
    private final String accessToken;

    public UserWithdrawalEvent(Long userId, String accessToken) {
        this.userId = userId;
        this.accessToken = accessToken;
    }

    public Long getUserId() { return userId; }
    public String getAccessToken() { return accessToken; }
}
