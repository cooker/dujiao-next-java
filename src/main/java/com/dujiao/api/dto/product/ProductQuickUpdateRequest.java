package com.dujiao.api.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductQuickUpdateRequest(
        @JsonProperty("is_active") Boolean active,
        @JsonProperty("sort_order") Integer sortOrder,
        @JsonProperty("category_id") Long categoryId) {}
