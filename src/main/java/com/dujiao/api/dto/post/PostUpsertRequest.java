package com.dujiao.api.dto.post;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 与 Go {@code admin.CreatePostRequest} 一致：{@code title} 等为 JSON 对象。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PostUpsertRequest(
        @NotBlank @Size(max = 200) String slug,
        @NotBlank @Size(max = 32) String type,
        @NotNull JsonNode title,
        JsonNode summary,
        JsonNode content,
        String thumbnail,
        @JsonProperty("is_published") Boolean published) {}
