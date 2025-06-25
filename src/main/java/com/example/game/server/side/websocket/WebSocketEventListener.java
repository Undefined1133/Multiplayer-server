package com.example.game.server.side.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    // A map to track subscribers by sessionId (or customize as needed)
    private Map<String, String> subscriptions = new HashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String playerId = headerAccessor.getFirstNativeHeader("playerId");
        if (playerId != null) {
            headerAccessor.getSessionAttributes().put("playerId", playerId);
            // Consider using that one instead of sending connection message ;)
//            messagingTemplate.convertAndSend("/topic/players/connected", playerId);
            logger.info("Player connected: " + playerId + " (session " + sessionId + ")");
        } else {
            logger.info("Received a new WebSocket connection with session ID: " + sessionId);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination(); // The destination the client subscribed to

        logger.info("New subscription to destination: " + destination + " from session: " + sessionId);

        // Store subscription details (if needed for later use)
        subscriptions.put(sessionId, destination);
    }

    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        logger.info("Unsubscribed from session: " + sessionId);

        // Remove from tracked subscriptions
        subscriptions.remove(sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        logger.info("WebSocket connection closed for session ID: " + sessionId);
        // Try to get playerId from session attributes
        final var playerIdObj = headerAccessor.getSessionAttributes().get("playerId");

        if (playerIdObj != null) {
            String playerId = playerIdObj.toString();
            logger.info("Player disconnected: " + playerId);
            messagingTemplate.convertAndSend("/topic/players/disconnected", playerId);
        } else {
            logger.info("Unknown player disconnected, session ID: " + sessionId);
            messagingTemplate.convertAndSend("/topic/players/disconnected", sessionId);
        }

        subscriptions.remove(sessionId);
    }

    // Utility method to log active subscriptions
    public void logActiveSubscriptions() {
        logger.info("Current Active Subscriptions: " + subscriptions);
    }
}
