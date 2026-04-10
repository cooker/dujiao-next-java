package com.dujiao.api.dto.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CategoryUpsertRequest(
        @JsonProperty("parent_id") Long parentId,
        @NotBlank String slug,
        @NotNull Map<String, Object> name,
        String icon,
        @JsonProperty("sort_order") Integer sortOrder) {}
