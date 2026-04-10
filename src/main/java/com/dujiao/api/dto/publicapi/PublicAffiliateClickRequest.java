package com.dujiao.api.dto.publicapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 与 Go {@code AffiliateTrackClickRequest} 对齐。 */
public record PublicAffiliateClickRequest(
        @NotBlank @JsonProperty("affiliate_code") String affiliateCode,
        @JsonProperty("visitor_key") String visitorKey,
        @JsonProperty("landing_path") String landingPath,
        String referrer) {}
