package com.dujiao.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiCredentialStatusRequest(@NotBlank @Size(max = 32) String status) {}
