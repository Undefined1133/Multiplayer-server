package com.example.game.server.side.service;

import com.example.game.server.side.model.Player;

public interface PlayerService {
    Player getPlayerInfo(String playerId);
}
