package com.dujiao.api.util;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** 文章 {@code title_json}/{@code summary_json}/{@code content_json} 与 Go {@code models.JSON} 互转。 */
public final class PostJsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PostJsonMapper() {}

    public static String toStoredJson(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    public static JsonNode toResponseNode(String stored) {
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
}
