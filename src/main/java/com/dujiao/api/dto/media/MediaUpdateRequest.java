package com.dujiao.api.dto.media;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

public record MediaUpdateRequest(
        @Size(max = 255) @JsonProperty("name") @JsonAlias("original_filename") String name) {}
