package com.dujiao.api.dto.media;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record MediaDto(
        long id,
        @JsonProperty("original_filename") String originalFilename,
        @JsonProperty("content_type") String contentType,
        @JsonProperty("size_bytes") long sizeBytes,
        String url,
        @JsonProperty("created_at") Instant createdAt) {}
