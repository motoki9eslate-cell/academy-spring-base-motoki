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

    // 入力された内容を、エラー後も画面に残す
    model.addAttribute("name", name);
    model.addAttribute("email", email);

    // 氏名
    if (name.isBlank()) {
        errors.add("氏名を入力してください");
    } else if (name.length() > 256) {
        errors.add("氏名は256文字以内で入力してください");
    }

    // メールアドレス
    if (email.isBlank()) {
        errors.add("メールアドレスを入力してください");
    } else if (!email.matches(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )) {
        errors.add("正しいメールアドレスを入力してください");
    }

    // パスワード
    if (password.isBlank()) {
        errors.add("パスワードを入力してください");
    } else if (!password.matches(
            "^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]{8,}$"
    )) {
        errors.add("パスワードは英数8文字以上で入力してください");
    }

    // 1つでもエラーがあれば登録せず、新規登録画面を再表示する
    if (!errors.isEmpty()) {
        model.addAttribute("errors", errors);
        return "register";
    }

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String hashedPassword = encoder.encode(password);

    jdbcTemplate.update(
            "INSERT INTO users (name, email, password) VALUES (?, ?, ?)",
            name.trim(),
            email.trim(),
            hashedPassword
    );

    return "redirect:/";
}
}