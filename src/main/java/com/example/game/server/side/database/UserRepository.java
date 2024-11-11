package com.example.game.server.side.database;

import com.example.game.server.side.database.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // You can define custom queries if needed, or use the built-in ones
}