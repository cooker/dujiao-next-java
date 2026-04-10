package com.dujiao.api.dto.user;

import jakarta.validation.constraints.NotBlank;

/** 与 Go {@code AffiliateWithdrawApplyRequest} 对齐，用于占位接口校验请求体。 */
public record AffiliateWithdrawApplyRequest(
        @NotBlank String amount, @NotBlank String channel, @NotBlank String account) {}
