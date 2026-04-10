package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record AdminSetMemberLevelRequest(
        @NotNull @JsonProperty("member_level_id") Long memberLevelId) {}
