package com.dujiao.api.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

/** 生成简单图形验证码图片（PNG Base64）。 */
@Service
public class CaptchaImageService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom R = new SecureRandom();
    private final ConcurrentHashMap<String, CaptchaEntry> entries = new ConcurrentHashMap<>();

    public Map<String, Object> generate(boolean enabled, int expireSeconds) {
        String captchaId = UUID.randomUUID().toString();
        String code = randomCode(5);
        String imageBase64 = renderBase64Png(code, 240, 80);
        int sec = Math.max(30, Math.min(expireSeconds, 3600));
        entries.put(captchaId, new CaptchaEntry(code, Instant.now().plusSeconds(sec)));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("captcha_id", captchaId);
        out.put("image_base64", imageBase64);
        out.put("enabled", enabled);
        return out;
    }

    public boolean verifyAndConsume(Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            return false;
        }
        Object idObj = map.get("captcha_id");
        Object codeObj = map.get("captcha_code");
        if (!(idObj instanceof String id) || !(codeObj instanceof String code)) {
            return false;
        }
        String captchaId = id.trim();
        String captchaCode = code.trim().toUpperCase();
        if (captchaId.isEmpty() || captchaCode.isEmpty()) {
            return false;
        }
        CaptchaEntry e = entries.remove(captchaId);
        if (e == null) {
            return false;
        }
        if (Instant.now().isAfter(e.expiresAt())) {
            return false;
        }
        return e.code().equalsIgnoreCase(captchaCode);
    }

    private static String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(R.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private static String renderBase64Png(String code, int width, int height) {
        try {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // background
            g.setColor(new Color(245, 248, 252));
            g.fillRect(0, 0, width, height);

            // noise lines
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(140 + R.nextInt(80), 140 + R.nextInt(80), 140 + R.nextInt(80)));
                g.drawLine(R.nextInt(width), R.nextInt(height), R.nextInt(width), R.nextInt(height));
            }

            // characters
            g.setFont(new Font("SansSerif", Font.BOLD, 42));
            int x = 20;
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(20 + R.nextInt(100), 30 + R.nextInt(120), 30 + R.nextInt(120)));
                int y = 50 + R.nextInt(18);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                x += 38 + R.nextInt(4);
            }
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private record CaptchaEntry(String code, Instant expiresAt) {}
}

