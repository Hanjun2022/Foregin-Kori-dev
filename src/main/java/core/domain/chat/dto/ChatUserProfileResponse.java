package core.domain.chat.dto;

import core.domain.user.entity.User;

public record ChatUserProfileResponse(
        Long id,
        String name,
        String firstName,
        String lastName,
        String sex,
        String birthdate,
        String country,
        String introduction,
        String purpose,
        String language,
        String hobby,
        String imageUrl
) {
    /**
     * User 엔티티와 이미지 URL 리스트를 받아서 UserProfileResponse 레코드를 생성하는 정적 팩토리 메서드
     */
    public static ChatUserProfileResponse from(User user,String imageUrls) {
        return new ChatUserProfileResponse(
                user.getId(),
                user.getName(),
                user.getFirstName(),
                user.getLastName(),
                user.getSex(),
                user.getBirthdate(),
                user.getCountry(),
                user.getIntroduction(),
                user.getPurpose(),
                user.getLanguage(),
                user.getHobby(),
                imageUrls
        );
    }
}