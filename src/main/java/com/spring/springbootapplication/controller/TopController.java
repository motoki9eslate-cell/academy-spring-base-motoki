package com.spring.springbootapplication.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.http.MediaType;

@Controller
public class TopController {

    private final JdbcTemplate jdbcTemplate;

    public TopController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

   @GetMapping("/")
public String top(HttpSession session, Model model) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    try {
        Map<String, Object> user = jdbcTemplate.queryForMap(
    """
    SELECT
        name,
        email,
        introduction,
        image_data
    FROM users
    WHERE email = ?
    """,
    loginUserEmail
);

        model.addAttribute("loginUser", user);
       boolean hasProfileImage = user.get("image_data") != null;
model.addAttribute("hasProfileImage", hasProfileImage);
    } catch (EmptyResultDataAccessException e) {
        session.invalidate();
        return "redirect:/login";
    }

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
        RedirectAttributes redirectAttributes,
        HttpSession session
) {
    try {
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT email, password FROM users WHERE email = ?",
                email.trim()
        );

            String hashedPassword = (String) user.get("password");
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (encoder.matches(password, hashedPassword)) {
             session.setAttribute("loginUserEmail", email);
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

        // メールアドレス重複チェック
if (!email.isBlank()) {
Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM users WHERE email = ?",
        Integer.class,
        email.trim()
);

if (count != null && count > 0) {
    errors.add("このメールアドレスは既に登録されています");
}
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
@PostMapping("/logout")
public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/login";
}

@GetMapping("/skills")
public String showSkills(
        @RequestParam(name = "month", required = false) Integer month,
        HttpSession session,
        Model model) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    java.time.LocalDate today = java.time.LocalDate.now();

    List<Integer> months = new ArrayList<>();

    for (int i = 0; i < 4; i++) {
        months.add(today.minusMonths(i).getMonthValue());
    }

    int selectedMonth = today.getMonthValue();

    if (month != null && months.contains(month)) {
        selectedMonth = month;
    }

    model.addAttribute("months", months);
    model.addAttribute("selectedMonth", selectedMonth);

    Integer userId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?",
            Integer.class,
            loginUserEmail
    );

    java.time.LocalDate selectedLearningMonth =
            today.withDayOfMonth(1);

    if (month != null) {
        for (int i = 0; i < 4; i++) {

            java.time.LocalDate candidate =
                    today.minusMonths(i).withDayOfMonth(1);

            if (candidate.getMonthValue() == month) {
                selectedLearningMonth = candidate;
                break;
            }
        }
    }

    List<Map<String, Object>> skillList = jdbcTemplate.queryForList(
            """
            SELECT
                c.id AS category_id,
                c.name AS category_name,
                s.id AS skill_id,
                s.name AS skill_name,
                COALESCE(ld.learning_minutes, 0) AS learning_minutes
            FROM categories c
            JOIN skills s
                ON c.id = s.category_id
            LEFT JOIN learning_data ld
                ON ld.skill_id = s.id
                AND ld.user_id = ?
                AND ld.learning_month = ?
            ORDER BY c.id, s.id
            """,
            userId,
            selectedLearningMonth
    );

    model.addAttribute("skillList", skillList);

    return "skills";
}

@GetMapping("/skills/new")
public String showNewSkill(
        @RequestParam Integer categoryId,
        @RequestParam Integer month,
        HttpSession session,
        Model model) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    String categoryName = jdbcTemplate.queryForObject(
            "SELECT name FROM categories WHERE id = ?",
            String.class,
            categoryId
    );

    model.addAttribute("categoryId", categoryId);
    model.addAttribute("categoryName", categoryName);
    model.addAttribute("month", month);

    return "newSkill";
}

@PostMapping("/skills/new")
public String addNewSkill(
        @RequestParam Integer categoryId,
        @RequestParam Integer month,
        @RequestParam(required = false) String skillName,
        @RequestParam(required = false) Integer learningMinutes,
        HttpSession session,
        Model model) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    boolean hasError = false;

model.addAttribute("categoryId", categoryId);
model.addAttribute("month", month);
model.addAttribute("skillName", skillName);
model.addAttribute("learningMinutes", learningMinutes);

String categoryName = jdbcTemplate.queryForObject(
        "SELECT name FROM categories WHERE id = ?",
        String.class,
        categoryId
);

model.addAttribute("categoryName", categoryName);

if (skillName == null || skillName.trim().isEmpty()) {

    model.addAttribute(
            "skillNameError",
            "項目名は必ず入力してください"
    );

    hasError = true;

} else if (skillName.trim().length() > 50) {

    model.addAttribute(
            "skillNameError",
            "項目名は50文字以内で入力してください"
    );

    hasError = true;
}

if (learningMinutes == null) {

    model.addAttribute(
            "learningMinutesError",
            "学習時間は必ず入力してください"
    );

    hasError = true;

} else if (learningMinutes < 0) {

    model.addAttribute(
            "learningMinutesError",
            "学習時間は0以上の数字で入力してください"
    );

    hasError = true;
}

if (hasError) {
    return "newSkill";
}

Integer duplicateCount = jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM skills
        WHERE category_id = ?
          AND name = ?
        """,
        Integer.class,
        categoryId,
        skillName == null ? "" : skillName.trim()
);

if (duplicateCount != null && duplicateCount > 0) {

    model.addAttribute(
            "skillNameError",
            skillName == null ? "" : skillName.trim() + "は既に登録されています"
    );

    return "newSkill";
}

    // ① skillsテーブルへ項目を追加
    jdbcTemplate.update(
            """
            INSERT INTO skills (category_id, name)
            VALUES (?, ?)
            """,
            categoryId,
            skillName == null ? "" : skillName.trim()
    );

    // ② 今追加したskillのidを取得
    Integer skillId = jdbcTemplate.queryForObject(
            """
            SELECT id
            FROM skills
            WHERE category_id = ?
              AND name = ?
            ORDER BY id DESC
            LIMIT 1
            """,
            Integer.class,
            categoryId,
            skillName == null ? "" : skillName.trim()
    );

    // ③ ログインユーザーのidを取得
    Integer userId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?",
            Integer.class,
            loginUserEmail
    );

    // ④ 選択された月をlearning_monthに変換
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate selectedLearningMonth = null;

    for (int i = 0; i < 4; i++) {

        java.time.LocalDate candidate =
                today.minusMonths(i).withDayOfMonth(1);

        if (candidate.getMonthValue() == month) {
            selectedLearningMonth = candidate;
            break;
        }
    }

    if (selectedLearningMonth == null) {
        return "redirect:/skills";
    }

    // ⑤ learning_dataへ学習時間を登録
    jdbcTemplate.update(
            """
            INSERT INTO learning_data
                (user_id, skill_id, learning_minutes, learning_month)
            VALUES (?, ?, ?, ?)
            """,
            userId,
            skillId,
            learningMinutes,
            selectedLearningMonth
    );

model.addAttribute("categoryId", categoryId);
model.addAttribute("categoryName", categoryName);
model.addAttribute("month", month);

model.addAttribute("registeredSkillName", skillName == null ? "" : skillName.trim());
model.addAttribute("registeredLearningMinutes", learningMinutes);

model.addAttribute("registrationComplete", true);

return "newSkill";
}

@PostMapping("/skills/save")
public String saveLearningTime(
        @RequestParam Integer skillId,
        @RequestParam Integer month,
        @RequestParam Integer learningMinutes,
        HttpSession session) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    if (learningMinutes < 0) {
        return "redirect:/skills?month=" + month;
    }

    Integer userId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?",
            Integer.class,
            loginUserEmail
    );

    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate selectedLearningMonth = null;

    for (int i = 0; i < 4; i++) {
        java.time.LocalDate candidate =
                today.minusMonths(i).withDayOfMonth(1);

        if (candidate.getMonthValue() == month) {
            selectedLearningMonth = candidate;
            break;
        }
    }

    if (selectedLearningMonth == null) {
        return "redirect:/skills";
    }

    jdbcTemplate.update(
            """
            INSERT INTO learning_data
                (user_id, skill_id, learning_minutes, learning_month)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (user_id, skill_id, learning_month)
            DO UPDATE SET
                learning_minutes = EXCLUDED.learning_minutes
            """,
            userId,
            skillId,
            learningMinutes,
            selectedLearningMonth
    );

    return "redirect:/skills?month=" + month;
}

@PostMapping("/skills/delete")
public String deleteSkill(
        @RequestParam Integer skillId,
        @RequestParam Integer month,
        HttpSession session) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    // 先に学習時間データを削除
    jdbcTemplate.update(
            """
            DELETE FROM learning_data
            WHERE skill_id = ?
            """,
            skillId
    );

    // その後、項目自体を削除
    jdbcTemplate.update(
            """
            DELETE FROM skills
            WHERE id = ?
            """,
            skillId
    );

    return "redirect:/skills?month=" + month;
}

@PostMapping("/skills/update")
public String updateLearningMinutes(
        @RequestParam Integer skillId,
        @RequestParam Integer month,
        @RequestParam Integer learningMinutes,
        HttpSession session) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    if (learningMinutes == null || learningMinutes < 0) {
        return "redirect:/skills?month=" + month;
    }

    Integer userId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = ?",
            Integer.class,
            loginUserEmail
    );

    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate selectedLearningMonth = null;

    for (int i = 0; i < 4; i++) {

        java.time.LocalDate candidate =
                today.minusMonths(i).withDayOfMonth(1);

        if (candidate.getMonthValue() == month) {
            selectedLearningMonth = candidate;
            break;
        }
    }

    if (selectedLearningMonth == null) {
        return "redirect:/skills";
    }

    Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM learning_data
            WHERE user_id = ?
              AND skill_id = ?
              AND learning_month = ?
            """,
            Integer.class,
            userId,
            skillId,
            selectedLearningMonth
    );

    if (count != null && count > 0) {

        jdbcTemplate.update(
                """
                UPDATE learning_data
                SET learning_minutes = ?
                WHERE user_id = ?
                  AND skill_id = ?
                  AND learning_month = ?
                """,
                learningMinutes,
                userId,
                skillId,
                selectedLearningMonth
        );

    } else {

        jdbcTemplate.update(
                """
                INSERT INTO learning_data
                    (user_id, skill_id, learning_minutes, learning_month)
                VALUES (?, ?, ?, ?)
                """,
                userId,
                skillId,
                learningMinutes,
                selectedLearningMonth
        );
    }

    return "redirect:/skills?month=" + month;
}

@GetMapping("/profile/edit")
public String showEditProfile(
        Model model,
        HttpSession session) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    String introduction = jdbcTemplate.queryForObject(
            "SELECT introduction FROM users WHERE email = ?",
            String.class,
            loginUserEmail
    );

    model.addAttribute(
            "introduction",
            introduction == null ? "" : introduction
    );

    return "editProfile";
}

@PostMapping("/profile/edit")
public String updateProfile(
        @RequestParam(name = "introduction", required = false)
        String introduction,
        @RequestParam(name = "image", required = false)
        MultipartFile image,
        Model model,
        HttpSession session) {

    String trimmedIntroduction =
            introduction == null ? "" : introduction.trim();

    if (trimmedIntroduction.length() < 50
            || trimmedIntroduction.length() > 200) {

        model.addAttribute(
                "errorMessage",
                "自己紹介は50文字以上200文字以下で入力してください"
        );
        model.addAttribute("introduction", introduction);

        return "editProfile";
    }

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return "redirect:/login";
    }

    try {
        if (image != null && !image.isEmpty()) {

            jdbcTemplate.update(
                    """
                    UPDATE users
                    SET introduction = ?,
                        image_path = ?,
                        image_data = ?,
                        image_content_type = ?
                    WHERE email = ?
                    """,
                    trimmedIntroduction,
                    image.getOriginalFilename(),
                    image.getBytes(),
                    image.getContentType(),
                    loginUserEmail
            );

        } else {

            jdbcTemplate.update(
                    """
                    UPDATE users
                    SET introduction = ?
                    WHERE email = ?
                    """,
                    trimmedIntroduction,
                    loginUserEmail
            );
        }

    } catch (IOException e) {
        model.addAttribute(
                "imageErrorMessage",
                "画像の保存に失敗しました"
        );
        model.addAttribute("introduction", introduction);

        return "editProfile";
    }

    return "redirect:/";
}

@GetMapping("/profile/image")
public ResponseEntity<byte[]> showProfileImage(
        HttpSession session) {

    String loginUserEmail =
            (String) session.getAttribute("loginUserEmail");

    if (loginUserEmail == null) {
        return ResponseEntity.notFound().build();
    }

    try {
        Map<String, Object> imageData = jdbcTemplate.queryForMap(
                """
                SELECT image_data, image_content_type
                FROM users
                WHERE email = ?
                """,
                loginUserEmail
        );

        byte[] imageBytes =
                (byte[]) imageData.get("image_data");

        String contentType =
                (String) imageData.get("image_content_type");

        if (imageBytes == null || contentType == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(imageBytes);

    } catch (EmptyResultDataAccessException e) {
        return ResponseEntity.notFound().build();
    }
}

}