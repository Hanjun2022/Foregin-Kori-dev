package core.domain.chat.entity;

import core.domain.user.entity.User;
import core.global.enums.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sent_at", updatable = false)
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    public ChatMessage(ChatRoom chatRoom, User sender, String content) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.sentAt = Instant.now();
        this.messageType = MessageType.TEXT;
    }
    public ChatMessage(ChatRoom chatRoom, User sender, String mediaKey, MessageType messageType) {
        if (messageType == MessageType.TEXT) {
            throw new IllegalArgumentException("미디어 메시지 생성자는 TEXT 타입을 허용하지 않습니다.");
        }
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = mediaKey;
        this.messageType = messageType;
        this.sentAt = Instant.now();
    }

}