package core.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FollowDTO {
    private Long id;
    private String firstname;
    private String lastname;
    private String gender;
    private String birthday;
    private String country;
    private String introduction;
    private String purpose;
    private String email;
    private List<String> language;
    private List<String> hobby;
    private String imageKey;
    private Long userId;




    // 풀 버전
    public FollowDTO(
            String firstname,
            String lastname,
            String gender,
            String birthday,
            String country,
            String introduction,
            String purpose,
            String email,
            List<String> language,
            List<String> hobby,
            String imageKey,

            Long userId
    ) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.gender = gender;
        this.birthday = birthday;
        this.country = country;
        this.introduction = introduction;
        this.purpose = purpose;
        this.email = email;
        this.language = language;
        this.hobby = hobby;
        this.imageKey = imageKey;
        this.userId = userId;
    }
}
