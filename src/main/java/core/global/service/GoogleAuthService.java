package core.global.service;


import core.domain.user.entity.User;
import core.domain.user.service.UserService;
import core.global.config.JwtTokenProvider;
import core.global.dto.AccessTokenDto;
import core.global.dto.GoogleProfileDto;
import core.global.dto.LoginResponseDto;
import core.global.dto.UserLoggedInEvent;
import core.global.enums.Ouathplatform;
import core.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleService googleService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public LoginResponseDto processGoogleLogin(String authCode) {
        AccessTokenDto accessTokenDto = googleService.exchangeCode(authCode);

        GoogleProfileDto profile = googleService.getGoogleProfile(accessTokenDto.getAccess_token());

        User user = findOrCreateUser(profile);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        Date expirationDate = jwtTokenProvider.getExpiration(refreshToken);
        long expirationMillis = expirationDate.getTime() - System.currentTimeMillis();
        redisService.saveRefreshToken(user.getId(), refreshToken, expirationMillis);

        boolean isNewUserResponse = user.isNewUser();

        publisher.publishEvent(new UserLoggedInEvent(user.getId().toString(), "google"));

        return new LoginResponseDto(user.getId(), accessToken, refreshToken, isNewUserResponse);
    }

    private User findOrCreateUser(GoogleProfileDto profile) {
        User user = userService.getUserBySocialIdAndProvider(profile.getSub(), String.valueOf(Ouathplatform.GOOGLE));
        if (user == null) {
            user = userService.createOauth(profile.getSub(), profile.getEmail(), String.valueOf(Ouathplatform.GOOGLE));
        }
        return user;
    }
}