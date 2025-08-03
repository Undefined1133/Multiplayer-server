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
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    // A map to track subscribers by sessionId (or customize as needed)
    private final Map<String, String> subscriptions = new HashMap<>();
    private final Map<String, String> podLocalPlayers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        final var headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        final var sessionId = headerAccessor.getSessionId();
        final var playerId = headerAccessor.getFirstNativeHeader("playerId");

        if (playerId != null && sessionId != null) {
            headerAccessor.getSessionAttributes().put("playerId", playerId);
            headerAccessor.getSessionAttributes().put("sessionId", sessionId);
            // Consider using that one instead of sending connection message ;)
            logger.info("Player connected: {}  (session {})", playerId, sessionId);
            podLocalPlayers.put(sessionId, playerId);
            final var payload = new HashMap<>();
            payload.put("ids", podLocalPlayers.values());
            logger.info("Values = {}", podLocalPlayers.values());
            messagingTemplate.convertAndSend("/topic/lobby-state", payload);
        } else {
            logger.info("Received a new WebSocket connection with session ID: {}", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        final var headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        final var sessionId = headerAccessor.getSessionId();
        final var destination = headerAccessor.getDestination(); // The destination the client subscribed to

        logger.info("New subscription to destination: {} from session: {}", destination, sessionId);

        // Store subscription details (if needed for later use)
        subscriptions.put(sessionId, destination);
    }

    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        final var headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        final var sessionId = headerAccessor.getSessionId();
        logger.info("Unsubscribed from session: {}", sessionId);

        // Remove from tracked subscriptions
        subscriptions.remove(sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        final var headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        final var sessionId = headerAccessor.getSessionId();
        logger.info("WebSocket connection closed for session ID: {}", sessionId);
        // Try to get playerId from session attributes
        final var playerIdObj = headerAccessor.getSessionAttributes().get("playerId");

        if (playerIdObj != null) {
            final var playerId = playerIdObj.toString();
            logger.info("Player disconnected with playerId: {} and sessionId: {}", playerId, sessionId);
            podLocalPlayers.remove(sessionId);
            messagingTemplate.convertAndSend("/topic/players/disconnected", playerId);
        } else {
            logger.info("Unknown player disconnected, session ID: {}", sessionId);
            messagingTemplate.convertAndSend("/topic/players/disconnected", sessionId);
        }

        subscriptions.remove(sessionId);
    }

    // Utility method to log active subscriptions
    public void logActiveSubscriptions() {
        logger.info("Current Active Subscriptions: {}", subscriptions);
    }
}
