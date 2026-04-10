package com.dujiao.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 与 Go {@code normalizeMultiLangJSON} / {@code models.Banner} 的 {@code title}/{@code subtitle}
 * JSON 列一致：仅保留 {@code zh-CN}/{@code zh-TW}/{@code en-US} 字符串值。
 */
public final class BannerI18nJson {

    public static final String[] SUPPORTED_LOCALES = {"zh-CN", "zh-TW", "en-US"};

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BannerI18nJson() {}

    /** 将请求中的 JSON 对象规范化为可入库的 JSON 字符串。 */
    public static String normalizeToStoredJson(JsonNode raw) {
        ObjectNode out = MAPPER.createObjectNode();
        for (String key : SUPPORTED_LOCALES) {
            String val = "";
            if (raw != null && raw.isObject() && raw.has(key)) {
                JsonNode n = raw.get(key);
                if (n != null && n.isTextual()) {
                    val = n.asText().trim();
                }
            }
            out.put(key, val);
        }
        try {
            return MAPPER.writeValueAsString(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 管理端/公开响应：库内 JSON 字符串 → 对象；兼容历史纯文本列。 */
    public static JsonNode storedToResponseNode(String stored) {
        if (stored == null || stored.isBlank()) {
            try {
                return MAPPER.readTree(normalizeToStoredJson(null));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        try {
            JsonNode n = MAPPER.readTree(stored);
            if (n.isObject()) {
                return n;
            }
        } catch (Exception ignored) {
        }
        ObjectNode o = MAPPER.createObjectNode();
        o.put("zh-CN", stored.trim());
        o.put("zh-TW", "");
        o.put("en-US", "");
        return o;
    }
}
