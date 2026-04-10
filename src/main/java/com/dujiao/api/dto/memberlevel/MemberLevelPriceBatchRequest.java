package com.dujiao.api.dto.memberlevel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record MemberLevelPriceBatchRequest(@NotEmpty @Valid List<MemberLevelPriceItem> items) {

    public record MemberLevelPriceItem(
            @NotNull @JsonProperty("member_level_id") Long memberLevelId,
            @NotNull @JsonProperty("product_id") Long productId,
            @NotNull @JsonProperty("price_amount") BigDecimal priceAmount) {}
}
