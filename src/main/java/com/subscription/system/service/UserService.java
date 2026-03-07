package com.subscription.system.service;

import com.subscription.system.models.User;
import com.subscription.system.repositories.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder)
    {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }
    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
    public User register(String name, String email, String rawPassword)
    {
        if(userRepo.existsByEmail(email))
        {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));

        return userRepo.save(user);
    }

    public User login(String email, String rawPassword)
    {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if(!passwordEncoder.matches(rawPassword, user.getPassword()))
        {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return user;
    }
}
