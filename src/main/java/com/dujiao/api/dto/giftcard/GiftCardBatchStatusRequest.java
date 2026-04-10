package com.dujiao.api.dto.giftcard;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GiftCardBatchStatusRequest(
        @NotEmpty @JsonProperty("ids") @JsonAlias("gift_card_ids") List<Long> ids,
        @NotEmpty @Size(max = 32) String status) {}
