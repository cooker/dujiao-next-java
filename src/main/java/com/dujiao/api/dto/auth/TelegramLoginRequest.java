package com.dujiao.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramLoginRequest(
        long id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String username,
        @JsonProperty("photo_url") String photoUrl,
        @JsonProperty("auth_date") long authDate,
        String hash) {}
