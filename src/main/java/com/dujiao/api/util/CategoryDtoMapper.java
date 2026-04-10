package com.dujiao.api.util;

import com.dujiao.api.domain.Category;
import com.dujiao.api.dto.category.CategoryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/** 与 Go {@code dto.NewCategoryResp} 一致：{@code name} 为多语言 JSON 对象。 */
public final class CategoryDtoMapper {

    private CategoryDtoMapper() {}

    public static CategoryDto from(Category c, ObjectMapper om) {
        if (c == null) {
            return null;
        }
        return new CategoryDto(
                c.getId(),
                c.getParentId(),
                c.getSlug(),
                nameMap(c, om),
                c.getIcon() == null ? "" : c.getIcon(),
                c.getSortOrder());
    }

    /** 多语言 {@code name} 对象（与 Go {@code Category.NameJSON} 一致）。 */
    public static Map<String, Object> nameMap(Category c, ObjectMapper om) {
        return readNameMap(c, om);
    }

    private static Map<String, Object> readNameMap(Category c, ObjectMapper om) {
        String raw = c.getNameJson();
        if (raw == null || raw.isBlank()) {
            return Map.of("zh-CN", c.getName() == null ? "" : c.getName());
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = om.readValue(raw, Map.class);
            return m == null || m.isEmpty()
                    ? Map.of("zh-CN", c.getName() == null ? "" : c.getName())
                    : m;
        } catch (Exception ignored) {
            return Map.of("zh-CN", c.getName() == null ? "" : c.getName());
        }
    }
}
