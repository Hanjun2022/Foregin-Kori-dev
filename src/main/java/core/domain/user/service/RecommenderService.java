package core.domain.user.service;

import core.domain.user.dto.UserProfileResponse;
import core.domain.user.entity.User;
import core.domain.user.repository.UserRepository;
import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommenderService {

    private final UserRepository userRepository;
    private final ContentBasedRecommender recommender;

    /**
     * 인증 객체에서 이메일을 추출하여 사용자 ID를 얻고 추천 로직을 실행합니다.
     * @param auth 인증 객체
     * @param limit 추천받을 사용자 수
     * @return 추천된 사용자 목록
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> recommendForUser(Authentication auth, int limit) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String email = auth.getName();
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_AVAILABLE);
        }

        // 이메일로 사용자 찾기
        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return recommender.recommendForUser(me.getId(), limit);
    }



}
