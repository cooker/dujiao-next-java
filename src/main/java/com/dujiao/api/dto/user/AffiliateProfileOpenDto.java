package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** 与 Go {@code AffiliateProfileResp} 对齐。 */
public record AffiliateProfileOpenDto(
        long id,
        String code,
        String status,
        @JsonProperty("created_at") Instant createdAt) {}
