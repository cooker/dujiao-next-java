package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** 管理端登录日志列表项（含审计字段）。 */
public record AdminLoginLogDto(
        long id,
        @JsonProperty("user_id") long userId,
        String email,
        String status,
        @JsonProperty("fail_reason") String failReason,
        @JsonProperty("client_ip") String clientIp,
        @JsonProperty("user_agent") String userAgent,
        @JsonProperty("login_source") String loginSource,
        @JsonProperty("request_id") String requestId,
        @JsonProperty("created_at") Instant createdAt) {}
