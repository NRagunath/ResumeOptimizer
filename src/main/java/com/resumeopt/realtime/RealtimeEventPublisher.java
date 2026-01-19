package com.resumeopt.realtime;

import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RealtimeEventPublisher {
    private final SimpMessagingTemplate template;

    public RealtimeEventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void publish(@NonNull String topic, @NonNull Object payload) {
        if (topic == null || payload == null) {
            throw new IllegalArgumentException("Topic and payload must not be null");
        }
        template.convertAndSend(topic, payload);
    }

    public void publishStatus(@NonNull String topic, @NonNull String status, @NonNull String message) {
        if (topic == null || status == null || message == null) {
            throw new IllegalArgumentException("Topic, status, and message must not be null");
        }
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("status", status);
        statusMap.put("message", message);
        statusMap.put("ts", System.currentTimeMillis());
        template.convertAndSend(topic, statusMap);
    }
}