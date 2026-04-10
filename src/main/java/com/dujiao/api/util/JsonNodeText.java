package com.dujiao.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** JSON 列：与 Go {@code models.JSON} 存库字符串对齐。 */
public final class JsonNodeText {

    private JsonNodeText() {}

    public static String toStored(ObjectMapper om, JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "{}";
        }
        try {
            return om.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }
}
