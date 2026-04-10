package com.dujiao.api.web;

import jakarta.servlet.http.HttpServletRequest;

/** 从 HTTP 请求提取客户端 IP、UA（与 Go gin 侧记录登录日志一致）。 */
public final class HttpClientSnapshot {

    private HttpClientSnapshot() {}

    public static String clientIp(HttpServletRequest req) {
        if (req == null) {
            return "";
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String rip = req.getRemoteAddr();
        return rip != null ? rip : "";
    }

    public static String userAgent(HttpServletRequest req) {
        if (req == null) {
            return "";
        }
        String ua = req.getHeader("User-Agent");
        return ua != null ? ua : "";
    }
}
