package com.dujiao.api.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ProductBatchIdsRequest(@JsonProperty("ids") @NotEmpty List<Long> ids) {}
