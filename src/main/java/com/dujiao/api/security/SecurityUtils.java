package com.dujiao.api.security;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static long requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal p)) {
            throw new BusinessException(ResponseCodes.UNAUTHORIZED, "unauthorized");
        }
        if (!"user".equals(p.typ())) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "forbidden");
        }
        return p.id();
    }

    public static long requireAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal p)) {
            throw new BusinessException(ResponseCodes.UNAUTHORIZED, "unauthorized");
        }
        if (!"admin".equals(p.typ())) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "forbidden");
        }
        return p.id();
    }

    /** 渠道 API（HMAC 签名）当前主体。 */
    public static ChannelPrincipal requireChannelPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ChannelPrincipal p)) {
            throw new BusinessException(ResponseCodes.UNAUTHORIZED, "channel_unauthorized");
        }
        return p;
    }
}
