package core.global.enums;

/**PENDING,     팔로우 요청이 보내졌지만, 아직 수락되지 않은 상태 (보낸 사람, 받은 사람 모두에게 해당)
ACCEPTED,    팔로우 요청이 수락되어 친구 관계가 성립된 상태
REJECTED     팔로우 요청이 거절된 상태 (선택 사항).
 **/
public enum FollowStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
