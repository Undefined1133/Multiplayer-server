package com.example.game.server.side.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TestPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public TestPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 5000) // Send a message every 5 seconds
    public void sendMessage() {
        String testMessage = "Ping from server at " + System.currentTimeMillis();
        messagingTemplate.convertAndSend("/topic/logs", testMessage);
        System.out.println("Sent ping message to /topic/logs: " + testMessage);
    }
}