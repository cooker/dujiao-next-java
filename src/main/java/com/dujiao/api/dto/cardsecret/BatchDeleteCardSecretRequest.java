package com.dujiao.api.dto.cardsecret;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BatchDeleteCardSecretRequest(
        java.util.List<Long> ids,
        @JsonProperty("batch_id") Long batchId,
        CardSecretQueryRequest filter) {}
