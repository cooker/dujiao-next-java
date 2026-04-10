package com.dujiao.api.dto.cardsecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record BatchUpdateCardSecretStatusRequest(
        java.util.List<Long> ids,
        @JsonProperty("batch_id") Long batchId,
        CardSecretQueryRequest filter,
        @NotBlank String status) {}
