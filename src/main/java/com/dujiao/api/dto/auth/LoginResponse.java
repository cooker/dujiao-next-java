package com.dujiao.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        String token,
        @JsonProperty("expires_at") Long expiresAt,
        @JsonProperty("user") UserBrief user) {

    public record UserBrief(long id, String email, String displayName) {}
}
