package com.securegate.iam.service;

import com.securegate.iam.model.SystemSetting;
import com.securegate.iam.repository.SystemSettingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
public class PasswordPolicyValidator {

    @Inject
    private SystemSettingRepository settingRepository;

    private static final int DEFAULT_MIN = 12;
    // Regex for complexity
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password cannot be empty");
        }

        // Fetch settings or defaults
        int minLength = getIntSetting("password_min_length", DEFAULT_MIN);
        boolean requireSpecial = getBoolSetting("password_require_special", true);
        boolean requireUpper = getBoolSetting("password_require_uppercase", true);
        boolean requireNumber = getBoolSetting("password_require_number", true);

        if (password.length() < minLength) {
            throw new BadRequestException("Password must be at least " + minLength + " characters long");
        }

        if (requireSpecial && !SPECIAL_CHAR.matcher(password).find()) {
            throw new BadRequestException("Password must contain at least one special character");
        }

        if (requireUpper && !UPPER_CASE.matcher(password).find()) {
            throw new BadRequestException("Password must contain at least one uppercase letter");
        }

        if (requireNumber && !DIGIT.matcher(password).find()) {
            throw new BadRequestException("Password must contain at least one number");
        }
    }

    private int getIntSetting(String key, int defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getSettingValue)
                .map(Integer::parseInt)
                .orElse(defaultValue);
    }

    private boolean getBoolSetting(String key, boolean defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getSettingValue)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }
}
