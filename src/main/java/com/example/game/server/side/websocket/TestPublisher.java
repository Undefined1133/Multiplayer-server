package com.example.game.server.side.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TestPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketEventListener webSocketEventListener;

    public TestPublisher(SimpMessagingTemplate messagingTemplate, WebSocketEventListener webSocketEventListener) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketEventListener = webSocketEventListener;
    }

    @Scheduled(fixedRate = 5000) // Send a message every 5 seconds
    public void sendMessage() {
        String testMessage = "Ping from server at " + System.currentTimeMillis();
        messagingTemplate.convertAndSend("/topic/logs", testMessage);
        webSocketEventListener.logActiveSubscriptions();
        System.out.println("Sent ping message to /topic/logs: " + testMessage);
    }
}