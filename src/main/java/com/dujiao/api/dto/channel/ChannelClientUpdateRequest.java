package com.dujiao.api.dto.channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChannelClientUpdateRequest(@NotBlank @Size(max = 200) String name) {}
