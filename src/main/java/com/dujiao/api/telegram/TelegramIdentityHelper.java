package com.dujiao.api.telegram;

import java.util.Locale;

public final class TelegramIdentityHelper {

    private static final String PLACEHOLDER_PREFIX = "telegram_";
    private static final String PLACEHOLDER_DOMAIN = "@login.local";
    private static final String DEFAULT_DISPLAY = "Telegram User";

    private TelegramIdentityHelper() {}

    public static boolean isPlaceholderEmail(String email) {
        if (email == null) {
            return false;
        }
        String n = email.toLowerCase(Locale.ROOT).trim();
        return n.startsWith(PLACEHOLDER_PREFIX) && n.endsWith(PLACEHOLDER_DOMAIN);
    }

    public static String placeholderEmail(String providerUserId) {
        String id = providerUserId == null ? "" : providerUserId.trim();
        if (id.isEmpty()) {
            id = "unknown";
        }
        return PLACEHOLDER_PREFIX + id + PLACEHOLDER_DOMAIN;
    }

    public static String resolveDisplayName(
            String providerUserId, String username, String firstName, String lastName) {
        String full =
                (firstName == null ? "" : firstName.trim())
                        + " "
                        + (lastName == null ? "" : lastName.trim());
        full = full.trim();
        if (!full.isEmpty()) {
            return full;
        }
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        if (providerUserId != null && !providerUserId.trim().isEmpty()) {
            return "telegram_" + providerUserId.trim();
        }
        return DEFAULT_DISPLAY;
    }
}
