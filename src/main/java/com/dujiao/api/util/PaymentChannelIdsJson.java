package com.dujiao.api.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** 与 Go {@code EncodeChannelIDs} / {@code payment_channel_ids} 文本列一致。 */
public final class PaymentChannelIdsJson {

    private PaymentChannelIdsJson() {}

    public static String encode(List<Long> ids, ObjectMapper om) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        try {
            return om.writeValueAsString(ids);
        } catch (Exception e) {
            return "";
        }
    }

    public static List<Long> decode(String raw, ObjectMapper om) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<Long> list = om.readValue(raw, new TypeReference<List<Long>>() {});
            return list == null ? List.of() : List.copyOf(list);
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
