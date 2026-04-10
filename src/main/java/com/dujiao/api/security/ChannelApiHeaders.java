package com.dujiao.api.security;

/** 与 Go {@code internal/router/channel_auth.go} 请求头一致。 */
public final class ChannelApiHeaders {

    private ChannelApiHeaders() {}

    public static final String CHANNEL_KEY = "Dujiao-Next-Channel-Key";
    public static final String CHANNEL_TIMESTAMP = "Dujiao-Next-Channel-Timestamp";
    public static final String CHANNEL_SIGNATURE = "Dujiao-Next-Channel-Signature";
}
