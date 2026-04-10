package com.dujiao.api.dto.giftcard;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GiftCardBatchStatusRequest(
        @JsonProperty("gift_card_ids") @NotEmpty List<Long> giftCardIds,
        @NotEmpty @Size(max = 32) String status) {}
