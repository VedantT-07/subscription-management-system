package com.subscription.system.controller;

import com.subscription.system.controller.dto.LoginRequest;
import com.subscription.system.controller.dto.RegisterRequest;
import com.subscription.system.models.User;
import com.subscription.system.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins ="*")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService)
    {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest req)
    {
        User user = userService.register(req.getEmail(), req.getEmail(), req.getPassword());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest req) {
        User user = userService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(user);
    }

}
