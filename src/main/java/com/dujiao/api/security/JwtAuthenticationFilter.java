package com.dujiao.api.security;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.crypto.ChannelHmac;
import com.dujiao.api.crypto.ChannelSecretCrypto;
import com.dujiao.api.domain.ChannelClientEntity;
import com.dujiao.api.repository.ChannelClientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private static final long MAX_CHANNEL_TIMESTAMP_SKEW_SEC = 60;

    private final JwtService jwtService;
    private final ChannelClientRepository channelClientRepository;
    private final ObjectMapper objectMapper;
    private final byte[] channelAesKey;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            ChannelClientRepository channelClientRepository,
            ObjectMapper objectMapper,
            @Value("${dujiao.crypto.master-key}") String masterKey) {
        this.jwtService = jwtService;
        this.channelClientRepository = channelClientRepository;
        this.objectMapper = objectMapper;
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException("dujiao.crypto.master-key is required");
        }
        this.channelAesKey = ChannelSecretCrypto.deriveKey(masterKey);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/v1/channel/")) {
            if (!processChannelSignature(request, response, filterChain)) {
                return;
            }
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String raw = header.substring(7).trim();
        if (raw.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtService.parse(raw);
            String typ = claims.get("typ", String.class);
            long id = Long.parseLong(claims.getSubject());
            List<SimpleGrantedAuthority> authorities =
                    "admin".equals(typ)
                            ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : List.of(new SimpleGrantedAuthority("ROLE_USER"));
            var auth =
                    new UsernamePasswordAuthenticationToken(
                            new JwtPrincipal(id, typ), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException ignored) {
            // 无效 token：不设置 SecurityContext，由后续鉴权处理
        }
        filterChain.doFilter(request, response);
    }

    /**
     * @return true 表示已继续链；false 表示已写响应并应终止
     */
    private boolean processChannelSignature(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return true;
        }

        String channelKey = request.getHeader(ChannelApiHeaders.CHANNEL_KEY);
        String timestampStr = request.getHeader(ChannelApiHeaders.CHANNEL_TIMESTAMP);
        String signature = request.getHeader(ChannelApiHeaders.CHANNEL_SIGNATURE);

        byte[] body = request.getInputStream().readAllBytes();
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, body);

        if (channelKey == null
                || channelKey.isBlank()
                || timestampStr == null
                || timestampStr.isBlank()
                || signature == null
                || signature.isBlank()) {
            writeUnauthorized(response);
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr.trim());
        } catch (NumberFormatException e) {
            writeUnauthorized(response);
            return false;
        }

        if (!isChannelTimestampValid(timestamp)) {
            writeUnauthorized(response);
            return false;
        }

        ChannelClientEntity client =
                channelClientRepository.findByClientId(channelKey.trim()).orElse(null);
        if (client == null) {
            writeUnauthorized(response);
            return false;
        }
        if (!"active".equalsIgnoreCase(client.getStatus())) {
            writeForbidden(response);
            return false;
        }

        String plainSecret;
        try {
            plainSecret = ChannelSecretCrypto.decrypt(channelAesKey, client.getSecretCipher());
        } catch (Exception e) {
            writeUnauthorized(response);
            return false;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();
        if (!ChannelHmac.verify(plainSecret, method, path, signature.trim(), timestamp, body)) {
            writeUnauthorized(response);
            return false;
        }

        var auth =
                new UsernamePasswordAuthenticationToken(
                        new ChannelPrincipal(client.getId(), client.getClientId()),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CHANNEL")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(wrapped, response);
        return true;
    }

    private static boolean isChannelTimestampValid(long timestamp) {
        long now = Instant.now().getEpochSecond();
        return Math.abs(now - timestamp) <= MAX_CHANNEL_TIMESTAMP_SKEW_SEC;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(ResponseCodes.UNAUTHORIZED, "unauthorized"));
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(), ApiResponse.error(ResponseCodes.FORBIDDEN, "forbidden"));
    }
}
