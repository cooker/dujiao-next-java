package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * 与 Go 管理端 {@code UpdateAdminUserRequest} 对齐：昵称字段为 {@code nickname}；同时接受
 * {@code display_name}。
 */
public record AdminUserUpdateRequest(
        @Size(max = 200)
                @JsonProperty("display_name")
                @JsonAlias("nickname")
                String displayName,
        String status) {}
