package com.dujiao.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminLoginResponse(
        String token,
        @JsonProperty("admin") AdminBrief admin) {

    public record AdminBrief(long id, String username, @JsonProperty("is_super") boolean superAdmin) {}
}
