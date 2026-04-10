package com.dujiao.api.dto.user;

import com.dujiao.api.domain.UserOAuthIdentityEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramBindingDto(
        boolean bound,
        String provider,
        @JsonProperty("provider_user_id") String providerUserId,
        String username,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("auth_at") String authAt) {

    public static TelegramBindingDto unbound() {
        return new TelegramBindingDto(false, null, null, null, null, null);
    }

    public static TelegramBindingDto fromEntity(UserOAuthIdentityEntity e) {
        if (e == null) {
            return unbound();
        }
        return new TelegramBindingDto(
                true,
                e.getProvider(),
                e.getProviderUserId(),
                e.getUsername(),
                e.getAvatarUrl(),
                null);
    }
}
