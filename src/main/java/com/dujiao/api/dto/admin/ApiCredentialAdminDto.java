package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiCredentialAdminDto(
        long id, @JsonProperty("user_id") long userId, @JsonProperty("api_key") String apiKey, String status) {}
