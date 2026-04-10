package com.dujiao.api.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** 与 Go {@code models.StringArray} / 商品 {@code images} JSON 数组一致。 */
public final class ProductImagesJson {

    private ProductImagesJson() {}

    /** 写入 {@code products.images}：{@code null} 或空列表序列化为 {@code []}。 */
    public static String toStoredJson(List<String> images, ObjectMapper om) {
        if (images == null || images.isEmpty()) {
            return "[]";
        }
        try {
            return om.writeValueAsString(images);
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
