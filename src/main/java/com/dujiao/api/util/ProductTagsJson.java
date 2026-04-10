package com.dujiao.api.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public final class ProductTagsJson {

    private ProductTagsJson() {}

    public static String toStored(List<String> tags, ObjectMapper om) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            return om.writeValueAsString(tags);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static List<String> parse(String raw, ObjectMapper om) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = om.readValue(raw, new TypeReference<List<String>>() {});
            return list == null ? List.of() : List.copyOf(list);
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
