package com.dujiao.api.dto.giftcard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GiftCardDto(
        long id,
        @JsonProperty("batch_id") Long batchId,
        String name,
        String code,
        String amount,
        String currency,
        String status,
        @JsonProperty("expires_at") Instant expiresAt,
        @JsonProperty("redeemed_at") Instant redeemedAt,
        @JsonProperty("redeemed_user_id") Long redeemedUserId,
        @JsonProperty("wallet_txn_id") Long walletTxnId,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {}
