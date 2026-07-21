package com.spring.springbootapplication.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String loginPost(
            @RequestParam String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT email, password FROM users WHERE email = ?",
                    email.trim()
            );

            String hashedPassword = (String) user.get("password");
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            if (encoder.matches(password, hashedPassword)) {
                return "redirect:/";
            }

        } catch (EmptyResultDataAccessException e) {
            // 該当するメールアドレスが存在しない場合も、
            // 同じエラーメッセージを表示する
        }

        redirectAttributes.addFlashAttribute(
                "loginError",
                "メールアドレス、もしくはパスワードが間違っています"
        );

        return "redirect:/login";
    }

    @PostMapping("/register")
    public String registerPost(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            Model model
    ) {
        List<String> errors = new ArrayList<>();

        // エラー後も入力内容を保持する
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

        // エラーがあれば新規登録画面を再表示する
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