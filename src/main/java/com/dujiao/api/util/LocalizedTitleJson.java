package com.dujiao.api.util;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;

/** 商品 {@code title} 与 Go {@code TitleJSON}（JSON 对象）互转及展示用解析。 */
public final class LocalizedTitleJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LocalizedTitleJson() {}

    /** 管理端请求体中的 {@code title}：JSON 对象为主；纯字符串时写入为 {@code {"zh-CN": "..."}}。 */
    public static String requestToStoredJson(JsonNode title) {
        if (title == null || title.isNull() || title.isMissingNode()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        try {
            if (title.isObject()) {
                return MAPPER.writeValueAsString(title);
            }
            if (title.isTextual()) {
                String t = title.asText().trim();
                if (t.isEmpty()) {
                    throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
                }
                ObjectNode o = MAPPER.createObjectNode();
                o.put("zh-CN", t);
                return MAPPER.writeValueAsString(o);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignored) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
    }

    /** 响应中的 {@code title}：序列化为 JSON 对象（与 Go 管理端/公开接口一致）。 */
    public static JsonNode storedToResponseNode(String stored) {
        if (stored == null || stored.isBlank()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(stored);
        } catch (Exception e) {
            ObjectNode o = MAPPER.createObjectNode();
            o.put("zh-CN", stored);
            return o;
        }
    }

    /** 订单行、购物车等单行展示。 */
    public static String storedToDisplayString(String stored) {
        if (stored == null || stored.isBlank()) {
            return "";
        }
        try {
            JsonNode n = MAPPER.readTree(stored);
            if (n.isTextual()) {
                return n.asText();
            }
            if (n.isObject()) {
                JsonNode zh = n.get("zh-CN");
                if (zh != null && zh.isTextual()) {
                    return zh.asText();
                }
                Iterator<String> it = n.fieldNames();
                while (it.hasNext()) {
                    JsonNode v = n.get(it.next());
                    if (v != null && v.isTextual()) {
                        return v.asText();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return stored;
    }
}
