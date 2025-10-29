package core.domain.user.dto;

// 패키지 예: core.domain.user.dto
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserSearchDTO {
    private Long   id;
    private String firstName;
    private String lastName;
    private String birthday;
    private String gender;
    private String country;
    private String introduction;
    private List<String> language;
    private String purpose;
    private List<String> hobby;
    private String imageKey;
}
