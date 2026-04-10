package com.dujiao.api.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 200) String displayName) {}
