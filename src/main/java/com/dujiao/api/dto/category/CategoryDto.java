package com.dujiao.api.dto.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record CategoryDto(
        long id,
        @JsonProperty("parent_id") long parentId,
        String slug,
        Map<String, Object> name,
        String icon,
        @JsonProperty("sort_order") int sortOrder) {}
