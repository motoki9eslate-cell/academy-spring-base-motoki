package com.spring.springbootapplication.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TopController {

    private final JdbcTemplate jdbcTemplate;

    public TopController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String top() {
        return "top";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
public String registerPost(
        @RequestParam String name,
        @RequestParam String email,
        @RequestParam String password
) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String hashedPassword = encoder.encode(password);

    jdbcTemplate.update(
            "INSERT INTO users (name, email, password) VALUES (?, ?, ?)",
            name,
            email,
            hashedPassword
    );

    return "redirect:/";
}
}