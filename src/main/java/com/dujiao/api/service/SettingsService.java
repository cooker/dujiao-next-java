package com.dujiao.api.service;

import com.dujiao.api.domain.SiteSetting;
import com.dujiao.api.repository.SiteSettingRepository;
import com.dujiao.api.service.settings.TelegramAuthSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final SiteSettingRepository siteSettingRepository;
    private final ObjectMapper objectMapper;

    public SettingsService(SiteSettingRepository siteSettingRepository, ObjectMapper objectMapper) {
        this.siteSettingRepository = siteSettingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSettingsMap() {
        return readKey("settings", defaultSettingsMap());
    }

    @Transactional
    public void putSettingsMap(Map<String, Object> map) {
        writeKey("settings", map);
    }

    /** 将 patch 合并进现有站点设置后持久化。 */
    @Transactional
    public void mergeSettingsMap(Map<String, Object> patch) {
        Map<String, Object> cur = new HashMap<>(getSettingsMap());
        cur.putAll(patch);
        writeKey("settings", cur);
    }

    @Transactional(readOnly = true)
    public JsonNode getJson(String key) {
        return siteSettingRepository
                .findById(key)
                .map(s -> parse(s.getValueJson()))
                .orElse(objectMapper.createObjectNode());
    }

    @Transactional
    public void putJson(String key, JsonNode node) {
        SiteSetting s = siteSettingRepository.findById(key).orElse(new SiteSetting());
        s.setSettingKey(key);
        s.setValueJson(node.toString());
        siteSettingRepository.save(s);
    }

    @Transactional
    public void resetOrderEmailTemplate() {
        putJson("order_email_template", objectMapper.createObjectNode());
    }

    /** 将 patch 合并进现有 JSON 对象（按字段覆盖），再持久化。 */
    @Transactional
    public void mergeJson(String key, JsonNode patch) {
        JsonNode cur = getJson(key);
        ObjectNode base =
                cur.isObject() ? (ObjectNode) cur.deepCopy() : objectMapper.createObjectNode();
        if (patch != null && patch.isObject()) {
            patch.fields().forEachRemaining(e -> base.set(e.getKey(), e.getValue()));
        }
        putJson(key, base);
    }

    private Map<String, Object> readKey(String key, Map<String, Object> defaults) {
        return siteSettingRepository
                .findById(key)
                .map(
                        st -> {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m =
                                        objectMapper.readValue(st.getValueJson(), Map.class);
                                return m;
                            } catch (Exception e) {
                                return defaults;
                            }
                        })
                .orElse(defaults);
    }

    private void writeKey(String key, Map<String, Object> map) {
        try {
            SiteSetting s = siteSettingRepository.findById(key).orElse(new SiteSetting());
            s.setSettingKey(key);
            s.setValueJson(objectMapper.writeValueAsString(map));
            siteSettingRepository.save(s);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private static Map<String, Object> defaultSettingsMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("site_name", "Dujiao-Next");
        m.put("registration_enabled", true);
        m.put("email_verification_enabled", false);
        return m;
    }

    @Transactional(readOnly = true)
    public boolean registrationEnabled() {
        return parseBool(getSettingsMap().get("registration_enabled"), true);
    }

    @Transactional(readOnly = true)
    public boolean emailVerificationEnabled() {
        return parseBool(getSettingsMap().get("email_verification_enabled"), false);
    }

    /** 与 Go captcha 配置语义兼容：provider!=none 或显式 enabled=true 视为启用。 */
    @Transactional(readOnly = true)
    public boolean captchaEnabled() {
        JsonNode n = getJson("captcha");
        String provider = n.path("provider").asText("none").trim().toLowerCase(Locale.ROOT);
        if (!"none".equals(provider)) {
            return true;
        }
        return n.path("enabled").asBoolean(false);
    }

    @Transactional(readOnly = true)
    public boolean captchaSceneEnabled(String scene) {
        if (scene == null || scene.isBlank()) {
            return false;
        }
        JsonNode n = getJson("captcha").path("scenes");
        return n.path(scene.trim()).asBoolean(false);
    }

    @Transactional(readOnly = true)
    public int captchaImageExpireSeconds() {
        int sec = getJson("captcha").path("image").path("expire_seconds").asInt(300);
        if (sec < 30) {
            return 30;
        }
        if (sec > 3600) {
            return 3600;
        }
        return sec;
    }

    @Transactional(readOnly = true)
    public TelegramAuthSettings telegramAuthSettings() {
        JsonNode n = getJson("telegram_auth");
        boolean enabled = n.path("enabled").asBoolean(false);
        String token = n.path("bot_token").asText("");
        int exp = n.path("login_expire_seconds").asInt(300);
        return new TelegramAuthSettings(enabled, token, exp);
    }

    /** 前台可见，不含 bot_token（与 Go {@code TelegramAuthService.PublicConfig} 一致）。 */
    @Transactional(readOnly = true)
    public Map<String, Object> telegramAuthPublicMap() {
        JsonNode n = getJson("telegram_auth");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", n.path("enabled").asBoolean(false));
        m.put("bot_username", n.path("bot_username").asText(""));
        m.put("mini_app_url", n.path("mini_app_url").asText(""));
        return m;
    }

    /** 与 Go {@code SettingKeyWalletConfig} / {@code GetWalletOnlyPayment} 一致。 */
    @Transactional(readOnly = true)
    public boolean walletOnlyPayment() {
        return getJson("wallet_config").path("wallet_only_payment").asBoolean(false);
    }

    /** 与 Go {@code GetWalletRechargeChannelIDs} 一致（{@code recharge_channel_ids}）。 */
    @Transactional(readOnly = true)
    public List<Long> walletRechargeChannelIds() {
        JsonNode arr = getJson("wallet_config").path("recharge_channel_ids");
        if (!arr.isArray()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (JsonNode x : arr) {
            if (x.isNumber()) {
                long v = x.longValue();
                if (v > 0) {
                    out.add(v);
                }
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public boolean affiliateEnabled() {
        return getJson("affiliate").path("enabled").asBoolean(false);
    }

    @Transactional(readOnly = true)
    public BigDecimal affiliateMinWithdrawAmount() {
        JsonNode n = getJson("affiliate").path("min_withdraw_amount");
        if (n.isMissingNode() || n.isNull()) {
            return new BigDecimal("0.01");
        }
        try {
            BigDecimal v =
                    n.isNumber()
                            ? n.decimalValue().setScale(2, RoundingMode.HALF_UP)
                            : new BigDecimal(n.asText().trim());
            return v.max(new BigDecimal("0.01"));
        } catch (Exception e) {
            return new BigDecimal("0.01");
        }
    }

    @Transactional(readOnly = true)
    public List<String> affiliateWithdrawChannels() {
        JsonNode arr = getJson("affiliate").path("withdraw_channels");
        if (!arr.isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonNode x : arr) {
            if (x.isTextual()) {
                String s = x.asText().trim().toLowerCase(Locale.ROOT);
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    /** 返利比例 0–100（与 Go {@code commission_rate} 一致）。 */
    @Transactional(readOnly = true)
    public BigDecimal affiliateCommissionRatePercent() {
        JsonNode n = getJson("affiliate").path("commission_rate");
        if (n.isMissingNode() || n.isNull()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal v =
                    n.isNumber()
                            ? n.decimalValue().setScale(2, RoundingMode.HALF_UP)
                            : new BigDecimal(n.asText().trim());
            if (v.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (v.compareTo(new BigDecimal("100")) > 0) {
                return new BigDecimal("100.00");
            }
            return v;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** 佣金确认天数（与 Go {@code confirm_days} 一致，0 表示支付后立即可提现）。 */
    @Transactional(readOnly = true)
    public int affiliateConfirmDays() {
        int d = getJson("affiliate").path("confirm_days").asInt(0);
        if (d < 0) {
            return 0;
        }
        if (d > 3650) {
            return 3650;
        }
        return d;
    }

    private static boolean parseBool(Object v, boolean defaultVal) {
        if (v == null) {
            return defaultVal;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return defaultVal;
        }
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }
}
