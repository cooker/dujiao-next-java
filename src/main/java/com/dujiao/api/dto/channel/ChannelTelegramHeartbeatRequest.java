package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** 与 Go {@code reportHeartbeatRequest} 对齐。 */
public record ChannelTelegramHeartbeatRequest(
        @JsonProperty("bot_version") String botVersion,
        @JsonProperty("webhook_status") String webhookStatus,
        @JsonProperty("machine_code") String machineCode,
        @JsonProperty("license_status") String licenseStatus,
        @JsonProperty("license_expires_at") String licenseExpiresAt,
        List<String> warnings) {}
