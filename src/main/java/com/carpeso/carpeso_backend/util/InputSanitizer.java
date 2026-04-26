package com.carpeso.carpeso_backend.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class InputSanitizer {

    // SQL Injection patterns
    private static final Pattern SQL_INJECTION = Pattern.compile(
            "(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION|SCRIPT|--|;|'|\")",
            Pattern.CASE_INSENSITIVE
    );

    // XSS patterns
    private static final Pattern XSS = Pattern.compile(
            "(?i)(<script|</script|javascript:|onerror=|onload=|onclick=|<iframe|<img|alert\\(|eval\\()",
            Pattern.CASE_INSENSITIVE
    );

    // Email pattern
    private static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    // Phone pattern — PH format
    private static final Pattern PHONE = Pattern.compile(
            "^(09|\\+639)\\d{9}$"
    );

    public boolean hasSqlInjection(String input) {
        if (input == null) return false;
        return SQL_INJECTION.matcher(input).find();
    }

    public boolean hasXss(String input) {
        if (input == null) return false;
        return XSS.matcher(input).find();
    }

    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL.matcher(email).matches();
    }

    public boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE.matcher(phone).matches();
    }

    public String sanitize(String input) {
        if (input == null) return null;
        return input
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;")
                .trim();
    }

    public void validateInput(String input, String fieldName) {
        if (input == null) return;
        if (hasSqlInjection(input)) {
            throw new RuntimeException("Invalid input detected in " + fieldName + "!");
        }
        if (hasXss(input)) {
            throw new RuntimeException("Invalid input detected in " + fieldName + "!");
        }
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars()
                .anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}