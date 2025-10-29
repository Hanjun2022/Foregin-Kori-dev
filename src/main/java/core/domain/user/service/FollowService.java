package core.domain.user.service;

import core.domain.notification.dto.NotificationEvent;
import core.domain.user.dto.FollowDTO;
import core.domain.user.entity.Follow;
import core.domain.user.entity.FollowActivityLog;
import core.domain.user.entity.User;
import core.domain.user.repository.FollowActivityLogRepository;
import core.domain.user.repository.FollowRepository;
import core.domain.user.repository.UserRepository;
import core.global.enums.*;
import core.global.exception.BusinessException;
import core.global.image.repository.ImageRepository;
import core.global.metrics.SocialChatMetrics;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ImageRepository imageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SocialChatMetrics socialChatMetrics;
    private final FollowActivityLogRepository followActivityLogRepository;

    private String countryOf(User u) {
        return Optional.ofNullable(u.getCountry()).orElse(null); // null/빈값은 SocialChatMetrics에서 UNK 처리
    }

    private void logFollowActivity(User follower, User following, FollowActionType actionType, String source) {
        FollowActivityLog log = FollowActivityLog.builder()
                .followerId(follower.getId())
                .followingId(following.getId())
                .actionType(actionType)
                .source(source)
                .followerIsInKorea(follower.isInKorea())
                .followerCountry(follower.getCountry())
                .followerSex(follower.getSex())
                .followerBirthdate(follower.getBirthdate())
                .followerLanguage(follower.getLanguage())
                .followingIsInKorea(following.isInKorea())
                .followingCountry(following.getCountry())
                .followingSex(following.getSex())
                .followingBirthdate(following.getBirthdate())
                .followingLanguage(following.getLanguage())
                .build();

        followActivityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getPendingFollowCounts(Authentication authentication) {
        User me = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        long sentCount = followRepository.countByUserIdAndStatus(me.getId(), FollowStatus.PENDING);
        long receivedCount = followRepository.countByFollowingIdAndStatus(me.getId(), FollowStatus.PENDING);

        Map<String, Long> result = new HashMap<>();
        result.put("sent", sentCount);
        result.put("received", receivedCount);

        return result;
    }

    /**
     * 친구 리스트 api
     */
    @Transactional(readOnly = true)
    public List<FollowDTO> getMyAcceptedFollows(Authentication authentication) {
        User me = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Follow> follows = followRepository.findAllAcceptedFollowsByUserId(me.getId(), FollowStatus.ACCEPTED);

        return follows.stream()
                .map(f -> {
                    User target = f.getUser().getId().equals(me.getId()) ? f.getFollowing() : f.getUser();

                    String imageKey = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, target.getId())
                            .map(image -> image.getUrl())
                            .orElse(null);

                    List<String> languages = Arrays.stream(
                                    Optional.ofNullable(target.getLanguage()).orElse("")
                                            .split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();

                    List<String> hobbies = Arrays.stream(
                                    Optional.ofNullable(target.getHobby()).orElse("")
                                            .split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();

                    return new FollowDTO(
                            target.getFirstName(),
                            target.getLastName(),
                            target.getSex(),
                            target.getBirthdate(),
                            target.getCountry(),
                            target.getIntroduction(),
                            target.getPurpose(),
                            target.getEmail(),
                            languages,
                            hobbies,
                            imageKey,
                            target.getId()
                    );
                })
                .toList();
    }
    /** 현재 로그인 사용자가 targetUserId를 팔로우 신청 */
    @Transactional
    public void follow(Authentication auth, Long targetUserId) {
        String email = auth.getName();

        User follower = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(follower.getBirthdate()==null||follower.getPurpose()==null||follower.getIntroduction()==null||follower.getLanguage()==null||follower.getHobby()==null||follower.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }


        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> {
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        if (follower.getId().equals(targetUser.getId())) {
            log.warn("[FOLLOW] 자기 자신 팔로우 시도 차단: 사용자={}", follower.getId());
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_YOURSELF);
        }

        Follow existing = followRepository.findByUserAndFollowing(follower, targetUser).orElse(null);
        Follow existingReverse = followRepository.findByUserAndFollowing(targetUser, follower).orElse(null);

        if (existing != null) {
            switch (existing.getStatus()) {
                case PENDING -> {
                    log.info("[FOLLOW] 이미 팔로우 신청 대기 중: from={}, to={}", follower.getId(), targetUser.getId());
                    throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS);
                }
                case ACCEPTED -> {
                    log.warn("[FOLLOW] 이미 팔로우 중: from={}, to={}", follower.getId(), targetUser.getId());
                    throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS);
                }
            }
        }
        if (existingReverse != null) {
            switch (existing.getStatus()) {
                case PENDING -> {
                    log.info("[FOLLOW] 이미 팔로우 신청 대기 중(Rev): from={}, to={}", targetUser.getId(), follower.getId());
                    throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS);
                }
                case ACCEPTED -> {
                    log.warn("[FOLLOW] 이미 팔로우 중(Rev): from={}, to={}", follower.getId(), targetUser.getId());
                    throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS);
                }
            }
        }

        Follow follow = Follow.builder()
                .user(follower)
                .following(targetUser)
                .status(FollowStatus.PENDING)
                .build();

        followRepository.save(follow);
        log.info("[FOLLOW] 팔로우 신청 성공: 신청자={}, 대상={}", follower.getId(), targetUser.getId());
        logFollowActivity(follower, targetUser, FollowActionType.REQUEST, "PROFILE");
        socialChatMetrics.recordFollowCreated(follower.getCountry(), targetUser.getCountry());

        NotificationEvent event = new NotificationEvent(
                targetUser.getId(),
                follower.getId(),
                NotificationType.receive,
                follow.getId(),
                null
        );

        eventPublisher.publishEvent(event);
    }

    /** 상대(fromUserId)가 보낸 팔로우 요청을 '현재 로그인 사용자'가 수락 */
    @Transactional
    public void acceptFollow(Authentication auth, Long fromUserId) {
        log.info("[ACCEPT FOLLOW] 요청 시작: 수락자={}, 신청자={}", auth.getName(), fromUserId);

        String toEmail = auth.getName();
        User toUser = userRepository.findByEmail(toEmail)
                .orElseThrow(() -> {
                    log.warn("[ACCEPT FOLLOW] 수락자 사용자 찾기 실패: email={}", toEmail);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> {
                    log.warn("[ACCEPT FOLLOW] 신청자 사용자 찾기 실패: 신청자 ID={}", fromUserId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        Follow follow = followRepository
                .findByUserAndFollowingAndStatus(fromUser, toUser, FollowStatus.PENDING)
                .orElseThrow(() -> {
                    log.warn("[ACCEPT FOLLOW] 대기 중인 팔로우 요청 없음: from={}, to={}", fromUser.getId(), toUser.getId());
                    return new BusinessException(ErrorCode.FOLLOWER_NOT_FOUND);
                });

        follow.accept();
        log.info("[ACCEPT FOLLOW] 팔로우 요청 수락 완료: 신청자={}, 수락자={}", fromUser.getId(), toUser.getId());
        logFollowActivity(fromUser, toUser, FollowActionType.ACCEPT, "NOTIFICATION");
        socialChatMetrics.recordFriendCreated(countryOf(toUser), countryOf(fromUser));

        NotificationEvent event = new NotificationEvent(
                fromUser.getId(),
                toUser.getId(),
                NotificationType.follow,
                follow.getId(),
                null
        );
        eventPublisher.publishEvent(event);
    }


    @Transactional
    public void unfollowAccepted(Authentication authentication, Long friendId) {
        log.info("unfollowAccepted 호출됨 - friendId: {}", friendId);

        User me = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> {
                    log.warn("로그인 사용자({})를 찾을 수 없음", authentication.getName());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        if (me.getId().equals(friendId)) {
            log.warn("사용자가 자기 자신을 언팔 시도 - userId: {}", me.getId());
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));


        Optional<Follow> targetFollow = followRepository.findByUser_IdAndFollowing_IdAndStatus(
                me.getId(), friendId, FollowStatus.ACCEPTED);

        Optional<Follow> targetInverseFollow = followRepository.findByUser_IdAndFollowing_IdAndStatus(
                friendId, me.getId(), FollowStatus.ACCEPTED);


        logFollowActivity(me, friend, FollowActionType.UNFOLLOW, "FRIEND_LIST");


        targetFollow.ifPresent(f -> {
            followRepository.delete(f);
            log.info("ACCEPTED 팔로우 삭제 완료 - {} -> {}", f.getUser().getId(), f.getFollowing().getId());
        });

        targetInverseFollow.ifPresent(f -> {
            followRepository.delete(f);
            log.info("ACCEPTED 팔로우 삭제 완료 - {} -> {}", f.getUser().getId(), f.getFollowing().getId());
        });

        if (targetFollow.isEmpty() && targetInverseFollow.isEmpty()) {
            log.warn("ACCEPTED 상태의 팔로우를 찾을 수 없음 - friendId: {}", friendId);
            throw new BusinessException(ErrorCode.FOLLOW_NOT_FOUND);
        }


    }

    /**
     *  RECEIVED/SENT 조회수 서비스
     */


    /** 현재 로그인 사용자가 targetUserId 언팔 */
    @Transactional
    public void unfollow(Authentication auth, Long targetUserId) {
        log.info("[UNFOLLOW] 요청 시작: 사용자={}, 대상={}", auth.getName(), targetUserId);

        String email = auth.getName();
        User follower = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[UNFOLLOW] 팔로워 사용자 찾기 실패: email={}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> {
                    log.warn("[UNFOLLOW] 대상 사용자 찾기 실패: 대상 ID={}", targetUserId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        Follow follow = followRepository.findByUserAndFollowing(follower, targetUser)
                .orElseThrow(() -> {
                    log.warn("[UNFOLLOW] 팔로우 관계 없음: from={}, to={}", follower.getId(), targetUser.getId());
                    return new BusinessException(ErrorCode.FOLLOWER_NOT_FOUND);
                });

        followRepository.delete(follow);
        log.info("[UNFOLLOW] 언팔로우 성공: from={}, to={}", follower.getId(), targetUser.getId());
    }


    /** 내(현재 로그인 사용자)가 보낸 사람(팔로잉) 나한테 메시지를 보낸사람 조회 */
    @Transactional(readOnly = true)
    public List<FollowDTO> getMyFollowsByStatus(Authentication auth, FollowStatus status, boolean isFollowers) {
        log.info("[GET FOLLOWS] 요청 시작: 사용자={}, 상태={}, 팔로워 조회 여부={}", auth.getName(), status, isFollowers);

        String email = auth.getName();
        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[GET FOLLOWS] 사용자 찾기 실패: email={}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        Stream<Follow> followStream;

        if (isFollowers) {
            followStream = followRepository.findByFollowingAndStatus(me, status).stream();
        } else { // false이면 내가 팔로우하는 사람들을 조회
            followStream = followRepository.findByUserAndStatus(me, status).stream();
        }

        List<FollowDTO> result = followStream
                .map(follow -> {
                    User targetUser = isFollowers ? follow.getUser() : follow.getFollowing();

                    String imageKey = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, targetUser.getId())
                            .map(image -> image.getUrl())
                            .orElse(null);
                    List<String> languages = (targetUser.getLanguage() != null && !targetUser.getLanguage().isBlank())
                            ? Arrays.stream(targetUser.getLanguage().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
                            : List.of();

                    List<String> hobbies = (targetUser.getHobby() != null && !targetUser.getHobby().isBlank())
                            ? Arrays.stream(targetUser.getHobby().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
                            : List.of();

                    return new FollowDTO(
                            targetUser.getFirstName(),
                            targetUser.getLastName(),
                            targetUser.getSex(),
                            targetUser.getBirthdate(),
                            targetUser.getCountry(),
                            targetUser.getIntroduction(),
                            targetUser.getPurpose(),
                            targetUser.getEmail(),
                            languages,
                            hobbies,
                            imageKey,
                            targetUser.getId()
                    );

                })
                .collect(Collectors.toList());

        log.info("[GET FOLLOWS] 조회 완료: 총 {}명의 사용자 반환", result.size());
        return result;
    }



    /** 상대(fromUserId)가 나에게 보낸 요청 거절 */
    @Transactional
    public void declineFollow(Authentication auth, Long fromUserId) {
        String toEmail = auth.getName();
        User toUser = userRepository.findByEmail(toEmail)
                .orElseThrow(() -> {
                    log.warn("[DECLINE FOLLOW] 거절자 사용자 찾기 실패: email={}", toEmail);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        Follow followReq = followRepository
                .findByUser_IdAndFollowing_IdAndStatus(fromUserId, toUser.getId(), FollowStatus.PENDING)
                .orElseThrow(() -> {
                    log.warn("[DECLINE FOLLOW] 대기 중인 요청 없음: from={}, to={}", fromUserId, toUser.getId());
                    return new BusinessException(ErrorCode.FOLLOWER_NOT_FOUND);
                });

        followRepository.delete(followReq);
        log.info("[DECLINE FOLLOW] 팔로우 요청 거절 완료: 신청자={}, 거절자={}", fromUserId, toUser.getId());
    }
}