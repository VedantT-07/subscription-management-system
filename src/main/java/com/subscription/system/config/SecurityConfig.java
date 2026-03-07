package com.subscription.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/**",              // 👈 TEMP: allow all APIs for dev
                                "/login.html",
                                "/register.html",
                                "/dashboard.html",
                                "/add-subscription.html",
                                "/subscriptions.html",
                                "/css/**",
                                "/js/**",
                                "/h2-console/**"
                        ).permitAll()
                        .anyRequest().permitAll()  // 👈 TEMP: open everything for dev
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
