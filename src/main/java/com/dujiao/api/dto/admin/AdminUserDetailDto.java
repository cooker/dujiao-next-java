package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminUserDetailDto(
        long id,
        String email,
        @JsonProperty("display_name") String displayName,
        String status,
        @JsonProperty("member_level_id") Long memberLevelId) {}
