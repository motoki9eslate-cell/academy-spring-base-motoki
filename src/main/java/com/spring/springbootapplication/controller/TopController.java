package com.spring.springbootapplication.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.ui.Model;
import java.util.ArrayList;
import java.util.List;

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
        @RequestParam String password,
        Model model
) {
    List<String> errors = new ArrayList<>();

    if (name.isBlank()) errors.add("氏名を入力してください");
    if (email.isBlank()) errors.add("メールアドレスを入力してください");
    if (password.isBlank()) errors.add("パスワードを入力してください");

    if (name.length() > 256) errors.add("氏名は256文字以内で入力してください");
    if (email.length() > 256) errors.add("メールアドレスは256文字以内で入力してください");
    if (password.length() > 256) errors.add("パスワードは256文字以内で入力してください");

    if (!errors.isEmpty()) {
        model.addAttribute("errors", errors);
        return "register";
    }

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