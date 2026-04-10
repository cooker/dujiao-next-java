package com.dujiao.api.dto.banner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** 管理端 Banner 详情/列表项，与 Go 返回的 {@code models.Banner} JSON 结构对齐。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BannerDto(
        long id,
        String name,
        String position,
        JsonNode title,
        JsonNode subtitle,
        String image,
        @JsonProperty("mobile_image") String mobileImage,
        @JsonProperty("link_type") String linkType,
        @JsonProperty("link_value") String linkValue,
        @JsonProperty("open_in_new_tab") boolean openInNewTab,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("start_at") Instant startAt,
        @JsonProperty("end_at") Instant endAt,
        @JsonProperty("sort_order") int sortOrder,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {}
