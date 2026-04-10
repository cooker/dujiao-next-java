package com.dujiao.api.dto.memberlevel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record MemberLevelPriceDto(
        long id,
        @JsonProperty("member_level_id") long memberLevelId,
        @JsonProperty("product_id") long productId,
        @JsonProperty("price_amount") BigDecimal priceAmount) {}
