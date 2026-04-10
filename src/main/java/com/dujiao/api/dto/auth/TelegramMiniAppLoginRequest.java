package com.dujiao.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramMiniAppLoginRequest(
        @JsonProperty("init_data") String initData, @JsonProperty("initData") String initDataCamel) {

    public String resolvedInitData() {
        if (initData != null && !initData.isBlank()) {
            return initData.trim();
        }
        return initDataCamel != null ? initDataCamel.trim() : "";
    }
}
