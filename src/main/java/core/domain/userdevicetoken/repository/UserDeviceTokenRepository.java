package core.domain.userdevicetoken.repository;

import core.domain.user.entity.User;
import core.domain.userdevicetoken.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    Optional<UserDeviceToken> findByDeviceToken(String deviceToken);

    List<UserDeviceToken> findAllByUserId(Long id);
    void deleteAllByUserId(Long userId);
    List<UserDeviceToken> findAllByUser(User user);
}