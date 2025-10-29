package core.domain.user.entity;

import core.domain.usernotificationsetting.entity.UserNotificationSetting;
import core.domain.notification.entity.Notification;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "social_id"}),
                @UniqueConstraint(columnNames = {"email"})
        })
@Getter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "first_name", nullable = true)
    private String firstName;

    @Column(name = "last_name", nullable = true)
    private String lastName;

    @Column(name = "SEX", nullable = true)
    private String sex;

    @Column(name = "birth_date", nullable = true)
    private String birthdate;

    @Column(name = "nationality", nullable = true)
    private String country;

    @Column(name = "introduction", length = 70)
    private String introduction;

    @Column(name = "visit_purpose", length = 40)
    private String purpose;

    @Column(name = "languages", nullable = true)
    private String language;

    @Column(name = "translate_language", nullable = true)
    private String translateLanguage;

    @Column(name = "hobby", nullable = true)
    private String hobby;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "provider", nullable = true)
    private String provider;

    @Column(name = "social_id", nullable = true)
    private String socialId;

    @Column(name = "email", nullable = true)
    private String email;

    @Column(name = "password", nullable = true)
    private String password;

    @Column(name = "is_new_user")
    private boolean isNewUser =true;

    @Column(name = "agreed_to_push_notification")
    private boolean agreedToPushNotification = false;

    @Column(name = "agreed_to_terms")
    private boolean agreedToTerms = false;
    @Column(name = "apple_refresh_token")
    private String appleRefreshToken;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "is_in_korea")
    private boolean isInKorea;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserNotificationSetting> notificationSettings = new ArrayList<>();
    @Builder
    public User(String firstName,
                String lastName,
                String sex,
                String birthdate,
                String country,
                String introduction,
                String purpose,
                String language,
                String hobby,
                String provider,
                String socialId,
                String email,String appleRefreshToken) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.sex = sex;
        this.birthdate = birthdate;
        this.country = country;
        this.introduction = introduction;
        this.purpose = purpose;
        this.language = language;
        this.hobby = hobby;
        this.provider = provider;
        this.socialId = socialId;
        this.email = email;
        this.appleRefreshToken = appleRefreshToken;
    }
    public void updateFirstName(String firstName) {
        if (notBlank(firstName)) this.firstName = firstName.trim();
        touchUpdatedAt();
    }

    public void updateLastName(String lastName) {
        if (notBlank(lastName)) this.lastName = lastName.trim();
        touchUpdatedAt();
    }

    public void updateSex(String sex) {
        if (sex != null) this.sex = sex;
        touchUpdatedAt();
    }

    public void updateBirthdate(String birthdate) {
        if (birthdate != null) this.birthdate = birthdate;
        touchUpdatedAt();
    }

    public void updateCountry(String country) {
        if (notBlank(country)) this.country = country.trim();
        touchUpdatedAt();
    }

    public void updateIntroduction(String introduction) {
        if (notBlank(introduction)) this.introduction = introduction.trim();
        touchUpdatedAt();
    }

    public void updatePurpose(String purpose) {
        if (notBlank(purpose)) this.purpose = purpose.trim();
        touchUpdatedAt();
    }

    public void updateLanguage(String language) {
        if (notBlank(language)) this.language = language;
        touchUpdatedAt();
    }

    public void updateTranslateLanguage(String translateLanguage) {
        if (notBlank(translateLanguage)) this.translateLanguage = translateLanguage;
        touchUpdatedAt();
    }

    public void updateHobby(String hobby) {
        if (notBlank(hobby)) this.hobby = hobby;
        touchUpdatedAt();
    }

    public void updatePassword(String password) {
        if (notBlank(password)) this.password = password;
        touchUpdatedAt();
    }

    public void updateEmail(String email) {
        if (notBlank(email)) this.email = email;
        touchUpdatedAt();
    }

    public void updateIsNewUser(boolean isNewUser) {
        this.isNewUser = isNewUser;
        touchUpdatedAt();
    }

    public void updateAgreedToPushNotification(boolean agreed) {
        this.agreedToPushNotification = agreed;
        touchUpdatedAt();
    }

    public void updateAgreedToTerms(boolean agreed) {
        this.agreedToTerms = agreed;
        touchUpdatedAt();
    }

    public void updateProvider(String provider) {
        if (notBlank(provider)) this.provider = provider;
        touchUpdatedAt();
    }

    public void updateSocialId(String socialId) {
        if (notBlank(socialId)) this.socialId = socialId;
        touchUpdatedAt();
    }
    public void updateLastSeenAt() {
        this.lastSeenAt = Instant.now();
    }
    public void updateCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void updateUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    public void updateIsInKorea(boolean isInKorea) {
        this.isInKorea = isInKorea;
        touchUpdatedAt();
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public void updateGender(String s) {
        this.sex = s;
    }

}
