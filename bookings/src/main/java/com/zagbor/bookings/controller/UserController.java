package com.zagbor.bookings.controller;

import com.zagbor.bookings.model.User;
import com.zagbor.bookings.service.AuthenticationService;
import com.zagbor.bookings.service.UserService;
import org.springframework.http.ResponseEntity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/user")

public class UserController {

    public final UserService userService;
    public final AuthenticationService authenticate;
    // Простейшее хранилище в памяти
    private final Map<String, String> users = new ConcurrentHashMap<>();
    private final Map<String, List<String>> roles = new ConcurrentHashMap<>();

    public UserController(PasswordEncoder passwordEncoder, UserService userService,
                          AuthenticationService authenticate) {
        this.userService = userService;
        this.authenticate = authenticate;
        // Добавим дефолтного пользователя для теста
        users.put("admin", passwordEncoder.encode("admin"));
        roles.put("admin", List.of("ADMIN"));
    }

    @PostMapping("/register")
    public User register(@RequestBody Map<String, Object> req) {
        String username = (String) req.get("username");
        String password = (String) req.get("password");
        boolean admin = req.getOrDefault("admin", false) instanceof Boolean b && b;
        return userService.createUser(username, password, admin);
    }

    @PostMapping("/auth")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> req) {
        String token = authenticate.authenticate(req.get("username"), req.get("password"));
        return ResponseEntity.ok(Map.of("access_token", token, "token_type", "Bearer"));
    }
}
