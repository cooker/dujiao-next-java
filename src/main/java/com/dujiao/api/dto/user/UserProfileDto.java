package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserProfileDto(
        long id,
        String email,
        String displayName,
        String status,
        @JsonProperty("member_level_id") Long memberLevelId,
        @JsonProperty("email_change_mode") String emailChangeMode,
        @JsonProperty("password_change_mode") String passwordChangeMode) {}
