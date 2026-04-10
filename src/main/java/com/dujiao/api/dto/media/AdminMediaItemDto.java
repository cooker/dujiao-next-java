package com.dujiao.api.dto.media;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record AdminMediaItemDto(
        long id,
        String name,
        String filename,
        String path,
        @JsonProperty("mime_type") String mimeType,
        long size,
        String scene,
        int width,
        int height,
        @JsonProperty("created_at") Instant createdAt) {}
