package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 与 Go {@code RedeemGiftCardRequest} 对齐；当 gift_card_redeem 场景启用验证码时需携带 captcha_payload。 */
public record RedeemGiftCardRequest(
        @NotBlank String code,
        @JsonProperty("captcha_payload") Object captchaPayload) {}
