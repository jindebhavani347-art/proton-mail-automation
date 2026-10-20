package com.qa.proton.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Single source of configuration. Resolution order: -D system property > environment variable > .env file > default.
 * Secrets (passwords) are never hard-coded; they come from .env locally or CI secrets.
 */
public final class Config {

    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private Config() {}

    public record Account(String role, String email, String password) {
        public Path storagePath() {
            return Path.of("auth", role + ".json");
        }
    }

    public static final String BASE_URL = get("BASE_URL", "https://mail.proton.me");
    public static final String LOGIN_URL = get("LOGIN_URL", "https://account.proton.me/mail");
    public static final String SETTINGS_URL = get("SETTINGS_URL", "https://account.proton.me/u/0/mail/filters");
    public static final boolean HEADLESS = Boolean.parseBoolean(get("HEADLESS", "true"));
    public static final String BROWSER = get("BROWSER", "chromium");
    public static final double TIMEOUT_MS = Double.parseDouble(get("TIMEOUT_MS", "20000"));
    public static final double LOGIN_TIMEOUT_MS = Double.parseDouble(get("LOGIN_TIMEOUT_MS", "120000"));
    public static final long DELIVERY_TIMEOUT_MS = Long.parseLong(get("MAIL_DELIVERY_TIMEOUT_MS", "90000"));

    public static final Account SENDER = new Account("sender", get("SENDER_EMAIL", ""), get("SENDER_PASSWORD", ""));
    public static final Account RECEIVER = new Account("receiver", get("RECEIVER_EMAIL", ""), get("RECEIVER_PASSWORD", ""));

    public static String get(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (isBlank(v)) v = System.getenv(key);
        if (isBlank(v)) v = DOT_ENV.get(key);
        return isBlank(v) ? defaultValue : v.trim();
    }

    public static void requireAccounts() {
        for (Account a : new Account[] {SENDER, RECEIVER}) {
            if (isBlank(a.email()) || isBlank(a.password())) {
                throw new IllegalStateException("Missing credentials for " + a.role()
                        + ". Copy .env.example to .env and fill SENDER_*/RECEIVER_* values.");
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> map = new HashMap<>();
        Path p = Path.of(".env");
        if (!Files.exists(p)) return map;
        try {
            for (String line : Files.readAllLines(p)) {
                String l = line.strip();
                if (l.isEmpty() || l.startsWith("#") || !l.contains("=")) continue;
                int i = l.indexOf('=');
                String value = l.substring(i + 1);
                int hash = value.indexOf(" #");           // strip inline comments
                if (hash >= 0) value = value.substring(0, hash);
                map.put(l.substring(0, i).strip(), value.strip());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read .env", e);
        }
        return map;
    }
}
