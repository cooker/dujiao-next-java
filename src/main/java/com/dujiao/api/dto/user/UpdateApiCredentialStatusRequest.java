package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 与 Go {@code UpdateMyApiCredentialStatusRequest} 对齐。 */
public record UpdateApiCredentialStatusRequest(@JsonProperty("is_active") boolean isActive) {}
