package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** 与 Go {@code dto.LoginLogResp} 字段对齐，便于后续接入登录日志表。 */
public record LoginLogDto(
        long id,
        String email,
        String status,
        @JsonProperty("client_ip") String clientIp,
        @JsonProperty("user_agent") String userAgent,
        @JsonProperty("login_source") String loginSource,
        @JsonProperty("created_at") Instant createdAt) {}
