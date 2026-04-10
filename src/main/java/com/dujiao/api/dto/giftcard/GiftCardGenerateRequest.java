package com.dujiao.api.dto.giftcard;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 与 Go {@code GenerateGiftCardsRequest} 对齐。 */
public record GiftCardGenerateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(1) @Max(10000) Integer quantity,
        @NotBlank String amount,
        @JsonProperty("expires_at") String expiresAt) {}
