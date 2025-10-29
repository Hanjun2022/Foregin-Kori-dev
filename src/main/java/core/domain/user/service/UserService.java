package core.domain.user.service;


import core.domain.bookmark.repository.BookmarkRepository;
import core.domain.chat.dto.ChatUserProfileResponse;
import core.domain.chat.entity.ChatParticipant;
import core.domain.chat.entity.ChatRoom;
import core.domain.chat.repository.ChatMessageRepository;
import core.domain.chat.repository.ChatParticipantRepository;
import core.domain.chat.repository.ChatRoomRepository;
import core.domain.comment.repository.CommentRepository;
import core.domain.notification.dto.NewUserJoinedEvent;
import core.domain.notification.repository.NotificationRepository;
import core.domain.post.entity.Post;
import core.domain.post.repository.BlockPostRepository;
import core.domain.post.repository.PostRepository;
import core.domain.user.dto.*;
import core.domain.user.entity.User;
import core.domain.user.repository.BlockRepository;
import core.domain.user.repository.FollowRepository;
import core.domain.user.repository.UserRepository;
import core.domain.userdevicetoken.repository.UserDeviceTokenRepository;
import core.domain.usernotificationsetting.repository.UserNotificationSettingRepository;
import core.global.config.JwtTokenProvider;
import core.global.dto.*;
import core.global.enums.ErrorCode;
import core.global.enums.ImageType;
import core.global.enums.Ouathplatform;
import core.global.exception.BusinessException;
import core.global.image.entity.Image;
import core.global.image.repository.ImageRepository;
import core.global.image.service.ImageService;
import core.global.like.repository.LikeRepository;
import core.global.service.AppleWithdrawalService;
import core.global.service.RedisService;
import core.global.service.SmtpMailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String EMAIL_VERIFY_CODE_KEY = "email_verification:code:";
    private static final String EMAIL_VERIFIED_FLAG_KEY = "email_verification:verified:";
    private static final long CODE_TTL_MIN = 3L;
    private static final long VERIFIED_TTL_MIN = 10L;
    /**
     * 8~12자, 특수문자(@/!/~) 1+ 포함, 허용문자 제한
     */
    private static final Pattern PW_RULE = Pattern.compile(
            "^(?=.*[@/!/~])[A-Za-z0-9@/!/~]{8,12}$"
    );
    private final BlockPostRepository blockPostRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final BlockRepository blockRepository;
    private final SmtpMailService smtpService;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CommentRepository commentRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final FollowRepository followRepository;
    private final LikeRepository likeRepository;
    private final AppleWithdrawalService appleWithdrawalService;
    private final ChatRoomRepository chatRoomRepository;
    private final ApplicationEventPublisher publisher;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    Pattern pattern = Pattern.compile("\\[(.*?)\\]");

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public User create(UserCreateDto memberCreateDto) {
        User user = User.builder()
                .email(memberCreateDto.getEmail())
                .build();
        userRepository.save(user);
        return user;
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User createOauth(String socialId, String email, String provider) {
        User u = User.builder()
                .socialId(socialId)
                .email(email)
                .provider(provider)
                .build();

        return userRepository.save(u);
    }

    @Transactional
    public User createAppleOauth(String socialId, String email, String provider, String appleRefreshToken
            , AppleLoginByCodeRequest.FullNameDto name) {
        User u = User.builder()
                .socialId(socialId)
                .email(email)
                .provider(provider)
                .appleRefreshToken(appleRefreshToken)
                .firstName(name.familyName())
                .lastName(name.givenName())
                .build();

        return userRepository.save(u);
    }

    /**
     * 사용자 정보(이름)를 업데이트합니다.
     *
     * @param user     업데이트할 User 엔티티
     * @param fullName Apple 로그인 시 전달받은 이름 정보 DTO
     */
    @Transactional
    public void updateUser(User user, AppleLoginByCodeRequest.FullNameDto fullName) {
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (fullName != null) {
            boolean isUpdated = false;
            if (fullName.givenName() != null && !fullName.givenName().isBlank()) {
                user.updateFirstName(fullName.givenName());
                isUpdated = true;
            }
            if (fullName.familyName() != null && !fullName.familyName().isBlank()) {
                user.updateLastName(fullName.familyName());
                isUpdated = true;
            }
            if (isUpdated) {
                userRepository.save(user);
            }
        }
    }

    public User getUserBySocialIdAndProvider(String socialId, String provider) {
        return userRepository.findByProviderAndSocialId(provider.trim(), socialId.trim()).orElse(null);
    }

    @Transactional
    public void setupUserProfile(UserSetupRequest dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("인증 정보 없음 - 이메일 프로필 업데이트 불가");
            throw new BusinessException(ErrorCode.EMAIL_NOT_AVAILABLE);
        }

        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!Objects.equals(user.getProvider(), Ouathplatform.APPLE.toString())) {

            if (notBlank(dto.firstname())) {
                user.updateFirstName(dto.firstname().trim());
            }
            if (notBlank(dto.lastname())) {
                user.updateLastName(dto.lastname().trim());
            }
        }
        if (dto.gender() != null) {
            user.updateSex(dto.gender());
        }
        if (dto.birthday() != null) {
            user.updateBirthdate(dto.birthday());
        }

        if (notBlank(dto.country())) {
            user.updateCountry(dto.country().trim());
        }

        if (notBlank(dto.introduction())) {
            String v = dto.introduction().trim();
            user.updateIntroduction(v.length() > 70 ? v.substring(0, 70) : v);
        }
        if (notBlank(dto.purpose())) {
            user.updatePurpose(dto.purpose());
        }


        if (dto.language() != null && !dto.language().isEmpty()) {
            List<String> normalizedLanguages = dto.language().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
            String userLanguagesCsv = String.join(",", normalizedLanguages);

            if (!userLanguagesCsv.isEmpty()) {
                user.updateLanguage(userLanguagesCsv);
            }

            String firstTranslatedLanguage = normalizedLanguages.stream()
                    .findFirst()
                    .map(s -> {
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.find()) {
                            return matcher.group(1).trim();
                        }
                        return "";
                    })
                    .orElse("");

            if (!firstTranslatedLanguage.isEmpty()) {
                user.updateTranslateLanguage(firstTranslatedLanguage);
            }
        }

        if (dto.hobby() != null) {
            String csv = dto.hobby().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(","));
            log.debug("취미 변경: {} → {}", user.getHobby(), csv);
            if (!csv.isEmpty()) user.updateHobby(csv);
        }
        user.updateIsNewUser(false);

        String finalImageKey = imageService.getUserProfileKey(user.getId());
        if (notBlank(dto.imageKey())) {
            finalImageKey = imageService.upsertUserProfileImage(user.getId(), dto.imageKey().trim());
        }

        UserSetupRequest result = new UserSetupRequest(
                user, stringToList(user.getLanguage()), stringToList(user.getHobby()), finalImageKey);

        log.info("newUser {}", user.isNewUser());
        log.info("프로필 업데이트 성공 반환: {}", result);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_AVAILABLE);
        }
        String email = (auth instanceof JwtAuthenticationToken jwtAuth)
                ? jwtAuth.getToken().getClaim("templates/email")
                : auth.getName();
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_AVAILABLE);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String profileKey = imageService.getUserProfileKey(user.getId());

        return new UserProfileResponse(user, stringToList(user.getTranslateLanguage()), stringToList(user.getHobby()), profileKey);
    }

    @Transactional
    public void deleteProfileImage() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_AVAILABLE);
        }
        String email = (auth instanceof JwtAuthenticationToken jwtAuth)
                ? jwtAuth.getToken().getClaim("templates/email")
                : auth.getName();
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_AVAILABLE);
        }


        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        imageService.deleteUserProfileImage(user.getId());
    }
    @Transactional
    public LoginResponseDto signup(SignupRequest req) {
        if (!req.isAgreedToTerms()) {
            throw new BusinessException(ErrorCode.AGREEMENT_INPUT);
        }

        String email = normalizeEmail(req.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }

        String verified = redisTemplate.opsForValue().get(EMAIL_VERIFIED_FLAG_KEY + email);
        if (!"1".equals(verified)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        }

        String rawPw = req.getPassword();

        User u = new User();
        u.updateProvider(Ouathplatform.local.toString());
        u.updateSocialId(buildLocalSocialId(email));
        u.updateEmail(email);
        u.updatePassword(passwordEncoder.encode(rawPw));
        u.updateIsNewUser(true);
        u.updateAgreedToTerms(req.isAgreedToTerms());
        u.updateAgreedToPushNotification(false);

        Instant now = Instant.now();
        u.updateCreatedAt(now);
        u.updateUpdatedAt(now);

        userRepository.save(u);

        redisTemplate.delete(EMAIL_VERIFIED_FLAG_KEY + email);

        String accessToken = jwtTokenProvider.createAccessToken(u.getId(), u.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(u.getId());

        Date expirationDate = jwtTokenProvider.getExpiration(refreshToken);
        long expirationMillis = expirationDate.getTime() - System.currentTimeMillis();
        redisService.saveRefreshToken(u.getId(), refreshToken, expirationMillis);

        publisher.publishEvent(new UserLoggedInEvent(u.getId().toString(), "local"));

        return new LoginResponseDto(u.getId(), accessToken, refreshToken, u.isNewUser());
    }


    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildLocalSocialId(String email) {
        // 결정적(동일 이메일이면 동일 결과) + 노출 안전하게 해시
        return "local:" + sha256Hex(email);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 일반 회원 가입 로직
     */
    @Transactional(readOnly = true)
    public AuthResponse login(EmailLoginDto req) {
        String email = normalizeEmail(req.getEmail());

        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[LOGIN] 사용자 없음: email={}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        log.debug("[LOGIN] 사용자 조회 성공: id={}, provider={}", u.getId(), u.getProvider());

        if (!Ouathplatform.local.toString().equalsIgnoreCase(nullToEmpty(u.getProvider()))) {
            log.warn("[LOGIN] provider 불일치: provider={}", u.getProvider());
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        }

        if (u.getPassword() == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            log.warn("[LOGIN] 비밀번호 불일치: email={}", email);
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        }

        String access = jwtTokenProvider.createAccessToken(u.getId(), u.getEmail());
        String refresh = jwtTokenProvider.createRefreshToken(u.getId());
        long expiresInMs = jwtTokenProvider.getExpiration(access).getTime() - System.currentTimeMillis();
        Date refreshExpiration = jwtTokenProvider.getExpiration(refresh);
        long refreshExpirationMillis = refreshExpiration.getTime() - System.currentTimeMillis();
        redisService.saveRefreshToken(u.getId(), refresh, refreshExpirationMillis);
        log.info("[LOGIN] 로그인 성공: id={}, email={}, expiresInMs={}", u.getId(), u.getEmail(), expiresInMs);

        publisher.publishEvent(new UserLoggedInEvent(u.getId().toString(), "email"));

        return new AuthResponse("Bearer", access, refresh, expiresInMs, u.getId(), u.getEmail(), u.isNewUser());
    }

    /**
     * 이메일 보내주는 로직
     */
    public void sendEmailVerificationCode(String rawEmail, Locale locale) {
        String email = normalizeEmail(rawEmail);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }

        Duration ttl = Duration.ofMinutes(CODE_TTL_MIN);

        String verificationCode = smtpService.sendVerificationEmail(
                email,
                ttl,
                locale
        );

        redisTemplate.opsForValue().set(
                EMAIL_VERIFY_CODE_KEY + email,
                verificationCode,
                CODE_TTL_MIN,
                TimeUnit.MINUTES
        );
    }

    /**
     * true 반환;
     */
    public boolean verifyEmailCode(EmailVerificationRequest request) {
        String email = normalizeEmail(request.getEmail());
        String verificationCode = request.getVerificationCode();

        String storedCode = redisTemplate.opsForValue().get(EMAIL_VERIFY_CODE_KEY + email);

        if (storedCode == null) {
            log.warn("Stored code not found for email: {}. Code may have expired.", email);
            return false;
        }

        if (!storedCode.equals(verificationCode)) {
            log.warn("Mismatched code for email: {}. Stored: {}, Received: {}", email, storedCode, verificationCode);
            return false;
        }

        // 사용한 코드는 즉시 폐기
        redisTemplate.delete(EMAIL_VERIFY_CODE_KEY + email);

        // 회원가입 시 사용할 인증 완료 플래그 저장(유예시간 부여)
        redisTemplate.opsForValue().set(
                EMAIL_VERIFIED_FLAG_KEY + email,
                "1",
                VERIFIED_TTL_MIN,
                TimeUnit.MINUTES
        );

        return true;
    }

    /**
     * 쉼표로 구분된 문자열을 List<String>으로 변환
     */
    private List<String> stringToList(String str) {
        if (str == null || str.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileEditDto updateUserProfile(UserProfileEditDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_AVAILABLE);
        }

        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (notBlank(dto.firstname())) user.updateFirstName(dto.firstname().trim());
        if (notBlank(dto.lastname())) user.updateLastName(dto.lastname().trim());
        if (dto.gender() != null) user.updateGender(dto.gender());
        if (dto.birthday() != null) user.updateBirthdate(dto.birthday());
        if (notBlank(dto.country())) user.updateCountry(dto.country().trim());

        if (notBlank(dto.introduction())) {
            String v = dto.introduction().trim();
            user.updateIntroduction(v.length() > 70 ? v.substring(0, 70) : v); // 컬럼 길이 보호
        }
        if (notBlank(dto.purpose())) {
            user.updatePurpose(dto.purpose());
        }

        if (dto.language() != null && !dto.language().isEmpty()) {
            List<String> languages = dto.language().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();

            if (!languages.isEmpty()) {
                // CSV 형태로 저장
                String userLanguagesCsv = String.join(",", languages);
                user.updateLanguage(userLanguagesCsv);

                // 첫 번째 요소에서 번역 코드 추출
                String firstTranslatedLanguage = languages.stream()
                        .map(s -> {
                            Matcher matcher = pattern.matcher(s);
                            return matcher.find() ? matcher.group(1).trim() : "";
                        })
                        .filter(s -> !s.isEmpty())
                        .findFirst()
                        .orElse("");

                if (!firstTranslatedLanguage.isEmpty()) {
                    user.updateTranslateLanguage(firstTranslatedLanguage);
                }
            }
        }

        if (dto.hobby() != null) {
            String csv = dto.hobby().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(","));
            if (!csv.isEmpty()) user.updateHobby(csv);
        }

        String finalImageKey = imageService.getUserProfileKey(user.getId());
        if (notBlank(dto.imageKey())) {
            finalImageKey = imageService.upsertUserProfileImage(user.getId(), dto.imageKey().trim());
        }

        return new UserProfileEditDto(user, stringToList(user.getLanguage()), stringToList(user.getHobby()), finalImageKey);
    }


    @Transactional
    public void updateUserSetup(UserUpdateDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_AVAILABLE);
        }

        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (dto.birthday() != null) user.updateBirthdate(dto.birthday());
        if (notBlank(dto.country())) user.updateCountry(dto.country().trim());

        if (notBlank(dto.introduction())) {
            String v = dto.introduction().trim();
            user.updateIntroduction(v.length() > 70 ? v.substring(0, 70) : v); // 컬럼 길이 보호
        }
        if (notBlank(dto.purpose())) {
            user.updatePurpose(dto.purpose());
        }

        if (dto.language() != null && !dto.language().isEmpty()) {
            List<String> languages = dto.language().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();

            if (!languages.isEmpty()) {
                // CSV 형태로 저장
                String userLanguagesCsv = String.join(",", languages);
                user.updateLanguage(userLanguagesCsv);

                // 첫 번째 요소에서 번역 코드 추출
                String firstTranslatedLanguage = languages.stream()
                        .map(s -> {
                            Matcher matcher = pattern.matcher(s);
                            return matcher.find() ? matcher.group(1).trim() : "";
                        })
                        .filter(s -> !s.isEmpty())
                        .findFirst()
                        .orElse("");

                if (!firstTranslatedLanguage.isEmpty()) {
                    user.updateTranslateLanguage(firstTranslatedLanguage);
                }
            }
        }

        if (dto.hobby() != null) {
            String csv = dto.hobby().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(","));
            if (!csv.isEmpty()) user.updateHobby(csv);
        }

        if (notBlank(dto.imageKey())) {
            imageService.upsertUserProfileImage(user.getId(), dto.imageKey().trim());
        }
        NewUserJoinedEvent event = new NewUserJoinedEvent(user.getId());
        eventPublisher.publishEvent(event);
    }


    /**
     * 두개의 이름 중 하나만 있더라도 바로 검색이 되게 함
     *
     * @param firstName
     * @param lastName
     * @return
     */
//    @Transactional(readOnly = true)
//    public List<UserSearchDTO> findUserByNameExcludingSelf(String firstName, String lastName)
//    {
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String userEmail = auth.getName();
//
//        User me = userRepository.findByEmail(userEmail)
//                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
//
//        if ((firstName == null || firstName.isBlank()) && (lastName == null || lastName.isBlank())) {
//            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
//        }
//
//
//        String fn = firstName == null ? null : firstName.trim();
//        String ln = lastName == null ? null : lastName.trim();
//
//        List<User>users;
//
//        if (notBlank(fn) && notBlank(ln)) {
//            users = userRepository
//                    .findAcceptedFriendsByFirstAndLastName(me.getId(),fn, ln);
//        } else if (notBlank(fn)) {
//            users = userRepository
//                    .findAcceptedFriendsByFirstAndLastName(me.getId(),fn, ln);
//        } else { // notBlank(ln) 보장됨
//            users = userRepository
//                    .findAcceptedFriendsByFirstAndLastName(me.getId(),fn, ln);
//        }
//
//        if (users.isEmpty()) {
//            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
//        }
//
//        return users.stream()
//                .map(this::toSearchDto)
//                .toList();
//    }

    /**
     * 현재 인증 컨텍스트에서 email만 확보 (ID는 repo로 조회)
     */


    private UserSearchDTO toSearchDto(User u) {
        return UserSearchDTO.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .gender(u.getSex())
                .country(u.getCountry())
                .imageKey(imageService.getUserProfileKey(u.getId())) // 필요 시 주석 해제
                .build();
    }

    /**
     * 회원 탈퇴를 처리하는 메서드.
     * 사용자와 관련된 모든 데이터를 삭제하고, 토큰을 무효화합니다.
     *
     * @param userId      탈퇴할 사용자의 ID
     * @param accessToken 블랙리스트에 추가할 사용자의 Access Token
     */
    /**
     * 회원 탈퇴 메인 메소드 (Orchestrator)
     */
    @Transactional
    public boolean withdrawUser(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        boolean isApple = false;

        if (Ouathplatform.APPLE.toString().equals(user.getProvider())) {
            appleWithdrawalService.revokeAppleToken(user);
            isApple = true;
        }

        cleanupUserData(user); // DB 작업들

        eventPublisher.publishEvent(new UserWithdrawalEvent(userId, accessToken));

        return isApple;
    }


    /**
     * 사용자와 관련된 모든 DB 데이터를 삭제하는 private 메소드
     */
    private void cleanupUserData(User user) {
        Long userId = user.getId();
        List<ChatRoom> ownedChatRooms = chatRoomRepository.findAllByOwnerId(userId);

        for (ChatRoom chatRoom : ownedChatRooms) {
            List<ChatParticipant> participants = chatParticipantRepository.findAllByChatRoomIdAndUserIdNot(chatRoom.getId(), userId);

            if (!participants.isEmpty()) {
                User newOwner = participants.get(0).getUser();
                chatRoom.changeOwner(newOwner);
                chatRoomRepository.save(chatRoom);
            } else {
                chatRoomRepository.delete(chatRoom);
            }
        }
        blockPostRepository.deleteAllBlockPostsRelatedToUser(userId);
        List<Post> userPosts = postRepository.findAllByAuthorId(userId);
        if (userPosts != null && !userPosts.isEmpty()) {
            commentRepository.deleteAllByPostIn(userPosts);
            bookmarkRepository.deleteAllByPostIn(userPosts);
            postRepository.deleteAll(userPosts);
        }

        commentRepository.deleteAllByAuthorId(userId);
        bookmarkRepository.deleteAllByUserId(userId);
        followRepository.deleteAllByUserId(userId);
        likeRepository.deleteAllByUserId(userId);
        imageRepository.deleteAllByImageTypeAndRelatedId(ImageType.USER, userId);
        imageService.deleteUserProfileImage(userId);
        blockRepository.deleteAllByUserOrBlocked(user);
        chatParticipantRepository.deleteAllByUserId(userId);
        chatMessageRepository.deleteAllBySenderId(userId);
        userNotificationSettingRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllByUserId(userId);
        userDeviceTokenRepository.deleteAllByUserId(userId);
        userRepository.delete(user);
    }

    /**
     * 단일 사용자 정보 조회 로직
     */
    public UserProfileResponse findUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));


        String profileKey = imageService.getUserProfileKey(user.getId());

        return new UserProfileResponse(user, stringToList(user.getTranslateLanguage()), stringToList(user.getHobby()), profileKey);
    }

    /**
     * 여러 사용자 정보 일괄 조회 로직 (N+1 문제 해결)
     */
    public List<UserResponseDto> findUsersProfiles(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userRepository.findAllById(userIds);
        List<Long> foundUserIds = users.stream().map(User::getId).toList();
        Map<Long, String> imageUrlsMap = imageRepository
                .findAllPrimaryImagesForUsers(ImageType.USER, foundUserIds)
                .stream()
                .collect(Collectors.toMap(Image::getRelatedId, Image::getUrl, (first, second) -> first));
        return users.stream()
                .map(user -> {
                    String imageUrl = imageUrlsMap.get(user.getId());
                    return UserResponseDto.from(user, imageUrl);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatUserProfileResponse getUserChatProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Image image = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        return ChatUserProfileResponse.from(user, image.getUrl());
    }

    /**
     * 사용자의 애플 계정 상태를 확인하는 메서드
     *
     * @param userId 확인할 사용자의 ID
     * @return UserAppleStatusResponse 사용자의 애플 계정 상태 정보
     */
    @Transactional(readOnly = true)
    public UserAppleStatusResponse checkUserAppleStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isApple = Ouathplatform.APPLE.toString().equals(user.getProvider());

        boolean isRejoiningWithoutFullName = false;
        if (isApple) {
            isRejoiningWithoutFullName = (user.getFirstName() == null || user.getFirstName().isBlank());
        }

        return new UserAppleStatusResponse(isApple, isRejoiningWithoutFullName);
    }
    @Transactional
    public void updateUserLocation(LocationUpdateRequest dto, Long userId ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Boolean isInKorea = isLocationInKorea(dto.getLatitude(), dto.getLongitude());
        user.updateIsInKorea(isInKorea);
    }

    /**
     * 주어진 위도, 경도가 대한민국 영토 내에 있는지 확인합니다.
     * @return 대한민국 내에 있으면 true, 밖에 있으면 false, 값이 없으면 null
     */
    private Boolean isLocationInKorea(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        double minLat = 33.0;
        double maxLat = 38.7;
        double minLon = 124.5;
        double maxLon = 132.0;

        return latitude >= minLat && latitude <= maxLat &&
                longitude >= minLon && longitude <= maxLon;
    }
}