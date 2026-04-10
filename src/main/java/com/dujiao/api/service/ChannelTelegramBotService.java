package com.dujiao.api.service;

import com.dujiao.api.dto.channel.ChannelTelegramHeartbeatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 渠道 Telegram Bot：拉取配置与心跳（与 Go {@code channel_telegram_bot.go} 对齐）。
 * 配置来自 {@code telegram_bot}；运行时状态来自 {@code telegram_bot_runtime_status}。
 */
@Service
public class ChannelTelegramBotService {

    static final String SK_TELEGRAM_BOT = "telegram_bot";
    static final String SK_TELEGRAM_RUNTIME = "telegram_bot_runtime_status";

    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;

    public ChannelTelegramBotService(SettingsService settingsService, ObjectMapper objectMapper) {
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChannelBotConfigPayload() {
        JsonNode raw = settingsService.getJson(SK_TELEGRAM_BOT);
        ObjectNode inner =
                raw != null && raw.isObject()
                        ? (ObjectNode) raw.deepCopy()
                        : objectMapper.createObjectNode();
        inner.put("bot_token", "");

        JsonNode runtime = settingsService.getJson(SK_TELEGRAM_RUNTIME);
        int outerVersion =
                runtime != null && runtime.hasNonNull("config_version")
                        ? runtime.path("config_version").asInt(0)
                        : inner.path("config_version").asInt(0);

        @SuppressWarnings("unchecked")
        Map<String, Object> configMap = objectMapper.convertValue(inner, Map.class);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("config", configMap);
        body.put("config_version", outerVersion);
        return body;
    }

    @Transactional
    public Map<String, Object> reportHeartbeat(ChannelTelegramHeartbeatRequest req) {
        JsonNode cur = settingsService.getJson(SK_TELEGRAM_RUNTIME);
        ObjectNode base =
                cur != null && cur.isObject()
                        ? (ObjectNode) cur.deepCopy()
                        : defaultRuntimeNode();

        String now = Instant.now().toString();
        base.put("connected", true);
        base.put("last_seen_at", now);
        if (req != null) {
            if (req.botVersion() != null) {
                base.put("bot_version", req.botVersion());
            }
            if (req.webhookStatus() != null) {
                base.put("webhook_status", req.webhookStatus());
            }
            if (req.machineCode() != null) {
                base.put("machine_code", req.machineCode());
            }
            if (req.licenseStatus() != null) {
                base.put("license_status", req.licenseStatus());
            }
            if (req.licenseExpiresAt() != null) {
                base.put("license_expires_at", req.licenseExpiresAt());
            }
            ArrayNode arr = objectMapper.createArrayNode();
            if (req.warnings() != null) {
                for (String w : req.warnings()) {
                    if (w != null && !w.isBlank()) {
                        arr.add(w);
                    }
                }
            }
            base.set("warnings", arr);
        }

        settingsService.putJson(SK_TELEGRAM_RUNTIME, base);
        int ver = base.path("config_version").asInt(0);
        return Map.of("config_version", ver);
    }

    private ObjectNode defaultRuntimeNode() {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("connected", false);
        n.put("last_seen_at", "");
        n.put("bot_version", "");
        n.put("webhook_status", "");
        n.put("machine_code", "");
        n.put("license_status", "");
        n.put("license_expires_at", "");
        n.set("warnings", objectMapper.createArrayNode());
        n.put("config_version", 0);
        n.put("last_config_sync_at", "");
        return n;
    }
}
