package com.dujiao.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 首次设置密码（Telegram 建号等）时 {@code old_password} 可省略。 */
public record ChangePasswordRequest(
        String oldPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword) {}
