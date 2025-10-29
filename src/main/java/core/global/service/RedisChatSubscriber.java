package core.global.service;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisChatSubscriber implements MessageListener {

    private final SimpMessagingTemplate simp;

    public RedisChatSubscriber(SimpMessagingTemplate simp) {
        this.simp = simp;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {

        String payload = new String(message.getBody());
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = om.readTree(payload);
            Long roomId = node.get("roomId").asLong();
            simp.convertAndSend("/topic/room." + roomId, payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}