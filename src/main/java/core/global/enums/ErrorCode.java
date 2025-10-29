package core.global.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "토큰이 없습니다."),
    JWT_EXPIRED(HttpStatus.BAD_REQUEST, "jwt 토큰이 만료되었습니다. "),
    INVALID_JWT(HttpStatus.BAD_REQUEST, "jwt 토큰을 찾을 수 없습니다."),
    ACCEPTED_EXISTS(HttpStatus.CONFLICT, "팔로우를 찾을수없습니다."),
    FOLLOW_NOT_FOUND(HttpStatus.CONFLICT, "팔로우를 찾을수없습니다."),
    INVALID_FOLLOW_STATUS(HttpStatus.CONFLICT, "팔로우상태가 아닙니다."),
    AGREEMENT_INPUT(HttpStatus.CONFLICT, "약관 동의가 필요합니다."),
    INVALID_EMAIL_INPUT(HttpStatus.BAD_REQUEST, "해당 이메일은 소셜 로그인 계정입니다. 소셜 로그인을 이용하세요."),
    DUPLICATE_RESOURCE(HttpStatus.FORBIDDEN, "중복된 이메일입니다."),
    AUTHENTICATION_OVER_FAILED(HttpStatus.FORBIDDEN, "횟수가 넘어갔습니다."),
    AUTHENTICATION_FAILED(HttpStatus.FORBIDDEN, "이메일 또는 비밀번호가 올바르지 않습니다."),
    PASSWORD_FORM_FAILED(HttpStatus.BAD_REQUEST, "비밀번호는 8~12자, 대/소문자 각 1자 이상 포함하고 특수문자(@/!/~)를 1개 이상 포함해야 합니다."),
    PASSWORD_NOT_CORRECTED(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    CANNOT_FOLLOW_YOURSELF(HttpStatus.BAD_REQUEST, "자기자신은  팔로우가 불가능합니다."),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다."),
    POST_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 글을 수정할 권한이 없습니다."),
    POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 글을 삭제할 권한이 없습니다."),
    NOT_AVAILABLE_ANONYMOUS(HttpStatus.CONFLICT, "익명이 허용되지 않는 카테고리입니다."),
    INVALID_BOARD_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리입니다."),
    NOT_AVAILABLE_WRITE(HttpStatus.CONFLICT, "쓰기가 불가능한 카테고리입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "대댓글하려는 댓글이 적합하지 않습니다."),
    INVALID_COMMENT_INPUT(HttpStatus.BAD_REQUEST, "댓글 입력값이 잘못됐습니다."),
    COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 댓글을 삭제할 권한이 없습니다."),
    COMMENT_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 댓글을 수정할 권한이 없습니다."),
    COMMENT_ALREADY_DELETED(HttpStatus.GONE, "삭제된 댓글입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    EMAIL_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "이메일이 이용 가능하지 않습니다."),
    FOLLOW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "You are already following this user."),
    INVALID_OR_EXPIRED_SESSION(HttpStatus.BAD_REQUEST, "세션이 만료되었습니다."),
    INVALID_OR_EXPIRED_TOKEN(HttpStatus.BAD_REQUEST, "토큰 시간이 만료되었습니다."),


    IMAGE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 처리 중 오류가 발생했습니다."),
    NO_MATCHING_APPLE_KEY(HttpStatus.BAD_REQUEST, "일치하는 Apple 공개키가 없습니다."),
    PUBLIC_KEY_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공개키 생성에 실패했습니다."),
    INVALID_JWT_ISSUER(HttpStatus.UNAUTHORIZED, "토큰 발급자(iss)가 유효하지 않습니다."),
    INVALID_JWT_AUDIENCE(HttpStatus.UNAUTHORIZED, "토큰 수신자(aud)가 유효하지 않습니다."),
    INVALID_JWT_NONCE(HttpStatus.UNAUTHORIZED, "Nonce 값이 일치하지 않습니다."),
    INVALID_JWT_APPLE(HttpStatus.UNAUTHORIZED, "APPLE JWT 값이 일치하지 않습니다."),
    INVALID_PRIVATE_KEY_APPLE(HttpStatus.UNAUTHORIZED, "APPLE 개인키가 유효하지 않습니다."),
    INVALID_APPLE_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "APPLE 리프레쉬 토큰을 지니고 있지 않습니다."),
    INVALID_APPLE_REQUEST(HttpStatus.UNAUTHORIZED, "APPLE 서버에 요청에 실패했습니다"),


    FOLLOWER_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로워를 찾을 수 없습니다."),
    PROFILE_IMAGE_REGISTER_REQUIRED(HttpStatus.BAD_REQUEST, "프로필에 사진 한 장 등록은 필수입니다."),
    PROFILE_IMAGE_ONLY_ONE(HttpStatus.BAD_REQUEST, "프로필은 한장만 등록 가능합니다."),
    UPDATE_NOT_PROCESSED(HttpStatus.BAD_REQUEST, "프로필 수정에 업데이트에 실패했습니다."),
    INVALID_USER_UPDATE_REQUEST(HttpStatus.BAD_REQUEST, "프로필 업데이트가 처리되지 않았습니다."),
    INVALID_OAUTH_CODE_MISSING(HttpStatus.BAD_REQUEST, "인가 코드가 누락되었습니다."),
    INVALID_OAUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 인가 코드입니다."),
    UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "해당 이메일은 이미 다른 소셜 계정으로 가입되어 있습니다."),
    OAUTH_PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OAuth 공급자에서 에러가 발생했습니다."),
    MISSING_SOCIAL_INFO(HttpStatus.BAD_REQUEST, "소셜 로그인 필수 정보가 누락되었습니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인 되어있지 않습니다."),
    WEAK_PASSWORD(HttpStatus.BAD_REQUEST, "보안에 취약합니다. 다시 재설정 바랍니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요가 존재하지 않습니다."),
    LIKE_ALREADY_EXIST(HttpStatus.CONFLICT, "좋아요가 이미 존재합니다."),
    BOOKMARK_ALREADY_EXIST(HttpStatus.CONFLICT, "북마크가 이미 존재합니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크가 존재하지 않습니다."),
    CHAT_ROOM_JOIN_FAILED(HttpStatus.BAD_REQUEST, "채팅방에 들어가지 못했습니다."),
    NOTIFICATION_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "알림 설정을 찾을 수 없습니다."),
    USER_NOTIFICATION_SETTING_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 알림 설정이 존재합니다."),

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지가 존재하지 않습니다."),
    FORBIDDEN_MESSAGE_DELETE(HttpStatus.BAD_REQUEST, "메세지는 보낸 사람만 삭제할 수 있습니다."),
    CHAT_PARTICIPANT_MINIMUM(HttpStatus.BAD_REQUEST, "채팅방에는 최소 1명(개설자)이 포함되어야 합니다."),
    CHAT_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여자 중 존재하지 않는 사용자가 있습니다."),
    NOT_AVAILABLE_LINK(HttpStatus.BAD_REQUEST, "링크가 불가능한 카테고리입니다."),
    FORBIDDEN_WORD_DETECTED(HttpStatus.BAD_REQUEST, "사용할 수 없는 단어가 포함되었습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "잘못된 커서값입니다."),

    IMAGE_STAGING_NOT_FOUND(HttpStatus.BAD_REQUEST, "스테이징 이미지가 존재하지 않습니다. 이미 이동되었을 수 있습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),
    IMAGE_COPY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 복사에 실패했습니다."),
    IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 삭제에 실패했습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_REQUEST, "이미지 업로드에 실패했습니다."),
    IMAGE_FILE_UPLOAD_TYPE_ERROR(HttpStatus.BAD_REQUEST, "이미지 파일이 아니라 다른 파일입니다."),
    IMAGE_FILE_DELETE_FAILED(HttpStatus.BAD_REQUEST, "이미지 삭제를 실패했습니다."),
    IMAGE_FOLDER_DELETE_FAILED(HttpStatus.BAD_REQUEST, "폴더 삭제에 실패했습니다."),
    CANNOT_BLOCK(HttpStatus.BAD_REQUEST, "차단할 수 없는 대상입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "otherUserId 형식이 잘못되었습니다."),
    TRANSLATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 번역에 실패했습니다."),
    CHAT_NOT_GROUP(HttpStatus.UNAUTHORIZED, "현재의 채팅방은 그룹채팅방이 아닙니다"),
    ALREADY_CHAT_PARTICIPANT(HttpStatus.UNAUTHORIZED, "이미 현재의 그룹채팅방에 참여하고 있습니다."),
    NOT_CHAT_PARTICIPANT(HttpStatus.UNAUTHORIZED, "유저는 현재 채팅방에 참여하고 있지 않습니다."),

    NOTIFICATION_FORBIDDEN(HttpStatus.UNAUTHORIZED, "알람을 받은 유저와 같은 유저가 아닙니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "해당 알람을 찾을 수 없습니다."),

    JWT_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증 정보(JWT)가 필요합니다."),
    JWT_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다."),
    JWT_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    JWT_TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "블랙리스트에 등록된 토큰입니다."),
    ELASTICSEARCH_INDEX_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "인덱스 작업에 실패했습니다."),
    ELASTICSEARCH_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "검색에 실패했습니다."),
    ELASTICSEARCH_SEARCH_SUGGEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "검색 제안에 실패했습니다."),
    BLOCKED_USER_POST(HttpStatus.CONFLICT, "접근할 수 없는 글입니다."),
    PROFILE_SET_NOT_COMPLETED(HttpStatus.PRECONDITION_REQUIRED, "프로필이 완성되지 않았습니다.");
    private final HttpStatus errorCode;
    private final String message;

}
