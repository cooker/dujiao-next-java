package com.dujiao.api.dto.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public record AdminCategoryDto(
        long id,
        @JsonProperty("parent_id") long parentId,
        String slug,
        Map<String, Object> name,
        String icon,
        @JsonProperty("sort_order") int sortOrder,
        @JsonProperty("created_at") Instant createdAt) {}
