package core.global.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
public class RedisChatPublisher {
    private final RedisTemplate<String, String> redisTemplate;
    private final ChannelTopic topic;

    public RedisChatPublisher(RedisTemplate<String, String> redisTemplate, ChannelTopic topic) {
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    public void publish(String messageJson) {
        redisTemplate.convertAndSend(topic.getTopic(), messageJson);
    }
}