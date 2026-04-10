package com.dujiao.api.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** 管理端文章 JSON，与 Go 返回的 {@code models.Post} 字段对齐。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostDto(
        long id,
        String slug,
        String type,
        JsonNode title,
        JsonNode summary,
        JsonNode content,
        String thumbnail,
        @JsonProperty("is_published") boolean published,
        @JsonProperty("published_at") Instant publishedAt,
        @JsonProperty("created_at") Instant createdAt) {}
