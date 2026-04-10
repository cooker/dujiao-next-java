package com.dujiao.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dujiao.jwt")
public class JwtProperties {

    private String secret = "change-me";
    private long expirationMs = 86_400_000L;
    /** “记住我”登录时的 JWT 有效期（默认 7 天）。 */
    private long rememberExpirationMs = 604_800_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public long getRememberExpirationMs() {
        return rememberExpirationMs;
    }

    public void setRememberExpirationMs(long rememberExpirationMs) {
        this.rememberExpirationMs = rememberExpirationMs;
    }
}
