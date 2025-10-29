package core.domain.user.service;

import core.domain.user.dto.UserProfileResponse;
import core.domain.user.entity.User;
import core.domain.user.repository.BlockRepository;
import core.domain.user.repository.FollowRepository;
import core.domain.user.repository.UserRepository;
import core.global.enums.ErrorCode;
import core.global.enums.FollowStatus;
import core.global.enums.ImageType;
import core.global.exception.BusinessException;
import core.global.image.repository.ImageRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentBasedRecommender {

    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;

    private static final double W_PURPOSE   = 0.35;
    private static final double W_COUNTRY   = 0.15;
    private static final double W_AGE       = 0.25;
    private static final double W_LANG      = 0.10;
    private static final double W_ACTIVITY  = 0.15;

    private static final double TEMPERATURE = 0.7;
    private static final double ACTIVITY_SCORE_HALF_LIFE_DAYS = 7.0;

    private static final java.security.SecureRandom RAND = new java.security.SecureRandom();

    @Transactional(readOnly = true)
    public List<UserProfileResponse> recommendForUser(Long meId, int limit) {
        User me = userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<FollowStatus> statusesToExclude = List.of(FollowStatus.PENDING, FollowStatus.ACCEPTED);
        Set<Long> followingIds = followRepository.findFollowingIdsByUserId(meId, statusesToExclude);
        Set<Long> blockedIds = blockRepository.findAllBlockedUserIds(meId);

        Set<Long> excludeIds = new HashSet<>(followingIds);
        excludeIds.addAll(blockedIds);
        excludeIds.add(meId);
        if (excludeIds.isEmpty()) {
            excludeIds.add(0L);
        }

        List<User> allCandidates = userRepository.findFullProfiledRecommendationCandidates(excludeIds);

        if (allCandidates.isEmpty()) {
            return List.of();
        }

        int meAge = safeAge(me.getBirthdate());
        Set<String> meLangs = csvToSet(me.getLanguage());

        List<Scored<User>> scored = allCandidates.stream()
                .map(candidate -> new Scored<>(candidate, score(me, candidate, meAge, meLangs)))
                .collect(Collectors.toList());

        List<User> chosen = pickGumbelTopK(scored, limit, TEMPERATURE);

        return chosen.stream().map(this::toDto).toList();
    }


    /** Gumbel-Top-k: key = score/T + Gumbel(0,1) 로 정렬 → 상위 limit 선택(중복 없음) */
    private List<User> pickGumbelTopK(List<Scored<User>> pool, int limit, double temperature) {
        class Draw { final User u; final double key; Draw(User u, double key){this.u=u; this.key=key;} }
        List<Draw> draws = new ArrayList<>(pool.size());
        double T = Math.max(1e-6, temperature);
        for (Scored<User> s : pool) {
            double u = RAND.nextDouble();
            double gumbel = -Math.log(-Math.log(u));
            double key = (s.score / T) + gumbel;
            draws.add(new Draw(s.item, key));
        }
        draws.sort((a, b) -> Double.compare(b.key, a.key));
        return draws.stream().limit(Math.max(1, limit)).map(d -> d.u).toList();
    }

    /** [수정] 활동 점수를 포함하고, 'me'의 불완전 프로필을 처리하는 최종 점수 계산 메서드 */
    private double score(User me, User other, int meAge, Set<String> meLangs) {
        double purposeScore = (me.getPurpose() == null || me.getPurpose().isBlank())
                ? 0.5 : (eq(me.getPurpose(), other.getPurpose()) ? 1.0 : 0.0);
        double countryScore = (me.getCountry() == null || me.getCountry().isBlank())
                ? 0.5 : (eq(me.getCountry(), other.getCountry()) ? 1.0 : 0.0);

        double ageScore = ageSim(meAge, safeAge(other.getBirthdate()), 9.0);
        double langScore = jaccard(meLangs, csvToSet(other.getLanguage()));
        double activityScore = calculateActivityScore(other, ACTIVITY_SCORE_HALF_LIFE_DAYS);
        return W_PURPOSE * purposeScore
                + W_COUNTRY * countryScore
                + W_AGE * ageScore
                + W_LANG * langScore
                + W_ACTIVITY * activityScore;
    }

    /** [추가] 사용자의 마지막 활동 시간(lastSeenAt)을 바탕으로 0.0 ~ 1.0 사이의 활동 점수를 계산 */
    private double calculateActivityScore(User user, double halfLifeDays) {
        if (user.getLastSeenAt() == null) {
            return 0.0;
        }
        long hoursSinceUpdate = ChronoUnit.HOURS.between(user.getLastSeenAt(), Instant.now());
        double daysSinceUpdate = hoursSinceUpdate / 24.0;
        double decayRate = Math.log(2) / halfLifeDays;
        return Math.exp(-decayRate * daysSinceUpdate);
    }
    private double ageSim(int a, int b, double sigma) {
        if (a <= 0 || b <= 0) return 0.5;
        return Math.exp(-Math.abs(a - b) / sigma);
    }

    private boolean eq(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private Set<String> csvToSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(s -> s.trim().toLowerCase())
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private double jaccard(Set<String> A, Set<String> B) {
        if (!A.isEmpty() || !B.isEmpty()) {
            Set<String> inter = new HashSet<>(A);
            inter.retainAll(B);
            Set<String> uni = new HashSet<>(A);
            uni.addAll(B);
            return uni.isEmpty() ? 0.0 : (double) inter.size() / (double) uni.size();
        }
        return 0.5;
    }

    private int safeAge(String birth) {
        if (birth == null || birth.isBlank()) {
            return -1;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            LocalDate d = LocalDate.parse(birth, formatter);
            return Period.between(d, LocalDate.now()).getYears();
        } catch (Exception e) {
            log.error(">>>> 잘못된 날짜 형식으로 나이 계산 실패: {}", birth, e);
            return -1;
        }
    }

    @Getter @AllArgsConstructor
    private static class Scored<T> { private T item; private double score; }


    private UserProfileResponse toDto(User u) {
        String imageKey = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, u.getId())
                .map(image -> image.getUrl())
                .orElse(null);

        return new UserProfileResponse(u,  csvToSet(u.getLanguage()).stream().toList(), csvToSet(u.getHobby()).stream().toList(),imageKey);
    }
}