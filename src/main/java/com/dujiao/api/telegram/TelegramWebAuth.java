package com.dujiao.api.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 与 Go {@code telegram_auth_service.go} 中 Telegram 登录 / Mini App 校验一致。 */
public final class TelegramWebAuth {

    private TelegramWebAuth() {}

    public static String buildLoginWidgetDataCheckString(
            long id,
            long authDate,
            String firstName,
            String lastName,
            String username,
            String photoUrl) {
        TreeMap<String, String> values = new TreeMap<>();
        values.put("auth_date", Long.toString(authDate));
        values.put("id", Long.toString(id));
        if (firstName != null && !firstName.isBlank()) {
            values.put("first_name", firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            values.put("last_name", lastName.trim());
        }
        if (username != null && !username.isBlank()) {
            values.put("username", username.trim());
        }
        if (photoUrl != null && !photoUrl.isBlank()) {
            values.put("photo_url", photoUrl.trim());
        }
        return joinDataCheck(values);
    }

    public static String buildMiniAppDataCheckString(Map<String, String> queryDecoded) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (var e : queryDecoded.entrySet()) {
            if ("hash".equals(e.getKey())) {
                continue;
            }
            sorted.put(e.getKey(), e.getValue());
        }
        return joinDataCheck(sorted);
    }

    private static String joinDataCheck(TreeMap<String, String> sorted) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var e : sorted.entrySet()) {
            if (!first) {
                sb.append('\n');
            }
            first = false;
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    /** Login Widget：secret = SHA256(bot_token)，再 HMAC-SHA256(data_check_string)。 */
    public static String loginWidgetHash(String botToken, String dataCheckString) throws Exception {
        byte[] secretKey = MessageDigest.getInstance("SHA-256").digest(botToken.trim().getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
        byte[] result = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(result);
    }

    /** Mini App：secret_key = HMAC_SHA256(key="WebAppData", bot_token)，再 HMAC-SHA256(data_check_string)。 */
    public static String miniAppHash(String botToken, String dataCheckString) throws Exception {
        Mac secretMac = Mac.getInstance("HmacSHA256");
        secretMac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] secret = secretMac.doFinal(botToken.trim().getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        byte[] result = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(result);
    }
}
