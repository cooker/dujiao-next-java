package com.dujiao.api.dto.banner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 与 Go {@code admin.BannerUpsertRequest} 字段一致；{@code title}/{@code subtitle} 为 JSON 对象，非必填字符串。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BannerUpsertRequest(
        @NotBlank @Size(max = 120) String name,
        String position,
        JsonNode title,
        JsonNode subtitle,
        @NotBlank @Size(max = 500) String image,
        @JsonProperty("mobile_image") String mobileImage,
        @JsonProperty("link_type") String linkType,
        @JsonProperty("link_value") String linkValue,
        @JsonProperty("open_in_new_tab") Boolean openInNewTab,
        @JsonProperty("is_active") Boolean active,
        @JsonProperty("start_at") String startAt,
        @JsonProperty("end_at") String endAt,
        @JsonProperty("sort_order") Integer sortOrder) {}
