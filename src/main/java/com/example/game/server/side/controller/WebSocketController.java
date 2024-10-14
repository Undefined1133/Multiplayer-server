package com.example.game.server.side.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.logging.Logger;

@Controller
public class WebSocketController {

    private static final Logger logger = Logger.getLogger(String.valueOf(WebSocketController.class));

    @MessageMapping("/send")
    @SendTo("/topic/logs")
    public String logAllMessages(String message) {
        logger.info("HELLO????" + message);
        return "Hello, here is ur message :) " + message;
    }
}