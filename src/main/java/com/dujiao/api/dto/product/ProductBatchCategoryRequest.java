package com.dujiao.api.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ProductBatchCategoryRequest(
        @JsonProperty("ids") @NotEmpty List<Long> ids,
        @JsonProperty("category_id") long categoryId) {}
