package com.dujiao.api.dto.cardsecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ExportCardSecretRequest(
        java.util.List<Long> ids,
        @JsonProperty("batch_id") Long batchId,
        CardSecretQueryRequest filter,
        @NotBlank String format) {}
