package com.dujiao.api.dto.channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChannelClientStatusRequest(
        @NotBlank
                @Pattern(
                        regexp = "^(active|disabled|suspended)$",
                        message = "invalid_status")
                String status) {}
