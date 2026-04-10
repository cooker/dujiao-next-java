package com.dujiao.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String CLAIM_TYP = "typ";
    private static final String TYP_USER = "user";
    private static final String TYP_ADMIN = "admin";

    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createUserToken(long userId) {
        return issueUserToken(userId, props.getExpirationMs()).token();
    }

    public IssuedToken issueUserToken(long userId, long expirationMs) {
        long now = System.currentTimeMillis();
        long expMs = now + expirationMs;
        String token =
                Jwts.builder()
                        .subject(String.valueOf(userId))
                        .claim(CLAIM_TYP, TYP_USER)
                        .issuedAt(new Date(now))
                        .expiration(new Date(expMs))
                        .signWith(key())
                        .compact();
        return new IssuedToken(token, expMs / 1000);
    }

    public String createAdminToken(long adminId) {
        return buildAdminToken(String.valueOf(adminId), TYP_ADMIN);
    }

    private String buildAdminToken(String subject, String typ) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYP, typ)
                .issuedAt(new Date(now))
                .expiration(new Date(now + props.getExpirationMs()))
                .signWith(key())
                .compact();
    }

    public record IssuedToken(String token, long expiresAtEpochSeconds) {}

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public boolean isAdmin(Claims claims) {
        return TYP_ADMIN.equals(claims.get(CLAIM_TYP, String.class));
    }

    public boolean isUser(Claims claims) {
        return TYP_USER.equals(claims.get(CLAIM_TYP, String.class));
    }
}
