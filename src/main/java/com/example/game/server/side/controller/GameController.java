package com.example.game.server.side.controller;

import com.example.game.server.side.model.Player;
import com.example.game.server.side.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GameController {

    private PlayerService playerService;

    @GetMapping("/player/{id}")
    public Player getPlayerInfo(@PathVariable("id") String playerId) {
        return playerService.getPlayerInfo(playerId);
    }

//    @PostMapping("/auth/login")
//    public ResponseEntity<?> loginPlayer(@RequestBody LoginRequest loginRequest) {
//        // Handle login
//    }
}