package core.global.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    post(true),
    comment(true),
    chat(true),
    follow(true),
    receive(true),
    newuser(true),
    followuserpost(true);

    private final boolean configurable;

    NotificationType(boolean configurable) {
        this.configurable = configurable;
    }


}