package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.dto.product.ProductSkuDto;
import com.dujiao.api.dto.product.ProductSkuRequest;
import com.dujiao.api.util.JsonNodeText;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

/**
 * 与 Go {@code syncSingleProductSKU} / {@code applyProductSKUsWithStockGuard} 对齐的 JDBC 实现；自动发货 SKU
 * 删除前<strong>未</strong>校验卡密库存（Java 侧暂无对应仓库）。
 */
@Component
public class AdminProductSkuJdbcSync {

    public static final String DEFAULT_SKU_CODE = "DEFAULT";
    private static final int MANUAL_UNLIMITED = -1;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AdminProductSkuJdbcSync(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public record SkuAggregate(
            List<NormalizedSkuRow> rows,
            BigDecimal minActivePrice,
            BigDecimal minActiveCost,
            int productManualStockTotal) {}

    public record NormalizedSkuRow(
            long id,
            String skuCode,
            String specJson,
            BigDecimal priceAmount,
            BigDecimal costPriceAmount,
            int manualStockTotal,
            boolean active,
            int sortOrder) {}

    public List<ProductSkuDto> listSkus(long productId) {
        String sql =
                """
                SELECT id, product_id, sku_code, spec_values::text AS spec_values, price_amount, cost_price_amount,
                       manual_stock_total, is_active, sort_order
                FROM product_skus
                WHERE product_id = :pid AND deleted_at IS NULL
                ORDER BY sort_order DESC, id ASC
                """;
        return jdbc.query(
                sql,
                new MapSqlParameterSource("pid", productId),
                (rs, rowNum) -> {
                    BigDecimal pa = rs.getBigDecimal("price_amount");
                    BigDecimal ca = rs.getBigDecimal("cost_price_amount");
                    return new ProductSkuDto(
                            rs.getLong("id"),
                            rs.getLong("product_id"),
                            rs.getString("sku_code"),
                            readSpecNode(rs.getString("spec_values")),
                            pa == null ? BigDecimal.ZERO : pa,
                            ca == null ? BigDecimal.ZERO : ca,
                            rs.getInt("manual_stock_total"),
                            0,
                            0L,
                            0,
                            rs.getBoolean("is_active"),
                            rs.getInt("sort_order"));
                });
    }

    private JsonNode readSpecNode(String raw) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    public Set<Long> existingSkuIds(long productId) {
        String sql =
                "SELECT id FROM product_skus WHERE product_id = :pid AND deleted_at IS NULL";
        return new HashSet<>(
                jdbc.query(sql, new MapSqlParameterSource("pid", productId), (rs, i) -> rs.getLong("id")));
    }

    public SkuAggregate normalizeSkuRequests(
            List<ProductSkuRequest> inputs, String fulfillmentType, Set<Long> existingIds) {
        if (inputs == null || inputs.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        boolean manual = "manual".equals(fulfillmentType);
        Set<String> seenCodes = new HashSet<>();
        List<NormalizedSkuRow> rows = new ArrayList<>();
        boolean hasActive = false;
        BigDecimal minPrice = null;
        BigDecimal minCost = null;
        int manualSum = 0;
        boolean hasUnlimited = false;

        for (ProductSkuRequest in : inputs) {
            String code = in.skuCode() == null ? "" : in.skuCode().trim();
            if (code.isEmpty()) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
            }
            String key = code.toLowerCase(Locale.ROOT);
            if (!seenCodes.add(key)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
            }
            long sid = in.id() == null ? 0L : in.id();
            if (sid > 0 && (existingIds == null || !existingIds.contains(sid))) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
            }
            if (in.priceAmount() == null) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "product_price_invalid");
            }
            BigDecimal price = in.priceAmount().setScale(2, RoundingMode.HALF_UP);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "product_price_invalid");
            }
            BigDecimal cost =
                    in.costPriceAmount() == null
                            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : in.costPriceAmount().setScale(2, RoundingMode.HALF_UP);
            if (cost.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "product_price_invalid");
            }
            int mstock = in.manualStockTotal() == null ? 0 : in.manualStockTotal();
            if (!manual) {
                mstock = 0;
            } else if (mstock < MANUAL_UNLIMITED) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "manual_stock_invalid");
            }
            boolean active = in.active() == null || in.active();
            int sort = in.sortOrder() == null ? 0 : in.sortOrder();
            String specJson = JsonNodeText.toStored(objectMapper, in.specValues());

            rows.add(
                    new NormalizedSkuRow(sid, code, specJson, price, cost, mstock, active, sort));

            if (active) {
                hasActive = true;
                if (minPrice == null || price.compareTo(minPrice) < 0) {
                    minPrice = price;
                }
                if (minCost == null || cost.compareTo(minCost) < 0) {
                    minCost = cost;
                }
                if (manual) {
                    if (mstock == MANUAL_UNLIMITED) {
                        hasUnlimited = true;
                    } else {
                        manualSum += mstock;
                    }
                }
            }
        }
        if (!hasActive) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        int productManual = 0;
        if (manual) {
            productManual = hasUnlimited ? MANUAL_UNLIMITED : manualSum;
        }
        if (minCost == null) {
            minCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new SkuAggregate(rows, minPrice, minCost, productManual);
    }

    public void syncSingleSku(
            long productId,
            BigDecimal priceAmount,
            BigDecimal costAmount,
            int manualStockTotal,
            boolean createWhenMissing) {
        String listSql =
                """
                SELECT id, sku_code, is_active, sort_order
                FROM product_skus
                WHERE product_id = :pid AND deleted_at IS NULL
                ORDER BY sort_order DESC, id ASC
                """;
        List<SkuRowLite> skus =
                jdbc.query(
                        listSql,
                        new MapSqlParameterSource("pid", productId),
                        (rs, rowNum) ->
                                new SkuRowLite(
                                        rs.getLong("id"),
                                        rs.getString("sku_code"),
                                        rs.getBoolean("is_active"),
                                        rs.getInt("sort_order")));
        if (skus.isEmpty()) {
            if (!createWhenMissing) {
                return;
            }
            insertSku(
                    productId,
                    DEFAULT_SKU_CODE,
                    "{}",
                    priceAmount,
                    costAmount,
                    manualStockTotal,
                    true,
                    0);
            return;
        }
        int targetIdx = pickSingleTargetIndex(skus);
        if (targetIdx < 0 || targetIdx >= skus.size()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        SkuRowLite target = skus.get(targetIdx);
        String code = target.skuCode();
        if (code == null || code.isBlank()) {
            code = DEFAULT_SKU_CODE;
        }
        String upd =
                """
                UPDATE product_skus SET sku_code = :code, price_amount = :price, cost_price_amount = :cost,
                manual_stock_total = :manual, is_active = true, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND product_id = :pid
                """;
        jdbc.update(
                upd,
                new MapSqlParameterSource()
                        .addValue("code", code)
                        .addValue("price", priceAmount)
                        .addValue("cost", costAmount)
                        .addValue("manual", manualStockTotal)
                        .addValue("id", target.id())
                        .addValue("pid", productId));
        for (int i = 0; i < skus.size(); i++) {
            if (i == targetIdx) {
                continue;
            }
            jdbc.update(
                    "DELETE FROM product_skus WHERE id = :id AND product_id = :pid",
                    new MapSqlParameterSource("id", skus.get(i).id()).addValue("pid", productId));
        }
    }

    private record SkuRowLite(long id, String skuCode, boolean active, int sortOrder) {}

    private int pickSingleTargetIndex(List<SkuRowLite> skus) {
        String def = DEFAULT_SKU_CODE.toUpperCase(Locale.ROOT);
        for (int i = 0; i < skus.size(); i++) {
            if (!skus.get(i).active()) {
                continue;
            }
            if (def.equals(skus.get(i).skuCode().trim().toUpperCase(Locale.ROOT))) {
                return i;
            }
        }
        for (int i = 0; i < skus.size(); i++) {
            if (skus.get(i).active()) {
                return i;
            }
        }
        for (int i = 0; i < skus.size(); i++) {
            if (def.equals(skus.get(i).skuCode().trim().toUpperCase(Locale.ROOT))) {
                return i;
            }
        }
        return 0;
    }

    public void applyMultiSku(long productId, List<NormalizedSkuRow> rows) {
        String listSql =
                """
                SELECT id, sku_code FROM product_skus WHERE product_id = :pid AND deleted_at IS NULL
                """;
        List<Map<String, Object>> existing =
                jdbc.queryForList(listSql, new MapSqlParameterSource("pid", productId));
        Map<Long, Map<String, Object>> byId = new HashMap<>();
        Map<String, Long> byCode = new HashMap<>();
        for (Map<String, Object> row : existing) {
            long id = ((Number) row.get("id")).longValue();
            String code = String.valueOf(row.get("sku_code"));
            byId.put(id, row);
            byCode.put(code.trim().toLowerCase(Locale.ROOT), id);
        }
        Set<Long> kept = new HashSet<>();
        for (NormalizedSkuRow row : rows) {
            if (row.id() > 0) {
                if (!byId.containsKey(row.id())) {
                    throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
                }
                String upd =
                        """
                        UPDATE product_skus SET sku_code = :code, spec_values = CAST(:spec AS jsonb),
                        price_amount = :price, cost_price_amount = :cost, manual_stock_total = :manual,
                        is_active = :active, sort_order = :sort, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND product_id = :pid
                        """;
                jdbc.update(
                        upd,
                        new MapSqlParameterSource()
                                .addValue("code", row.skuCode())
                                .addValue("spec", row.specJson())
                                .addValue("price", row.priceAmount())
                                .addValue("cost", row.costPriceAmount())
                                .addValue("manual", row.manualStockTotal())
                                .addValue("active", row.active())
                                .addValue("sort", row.sortOrder())
                                .addValue("id", row.id())
                                .addValue("pid", productId));
                kept.add(row.id());
                continue;
            }
            String ck = row.skuCode().trim().toLowerCase(Locale.ROOT);
            Long existingId = byCode.get(ck);
            if (existingId != null) {
                String upd =
                        """
                        UPDATE product_skus SET spec_values = CAST(:spec AS jsonb), price_amount = :price,
                        cost_price_amount = :cost, manual_stock_total = :manual, is_active = :active,
                        sort_order = :sort, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND product_id = :pid
                        """;
                jdbc.update(
                        upd,
                        new MapSqlParameterSource()
                                .addValue("spec", row.specJson())
                                .addValue("price", row.priceAmount())
                                .addValue("cost", row.costPriceAmount())
                                .addValue("manual", row.manualStockTotal())
                                .addValue("active", row.active())
                                .addValue("sort", row.sortOrder())
                                .addValue("id", existingId)
                                .addValue("pid", productId));
                kept.add(existingId);
            } else {
                long newId = insertSku(
                        productId,
                        row.skuCode(),
                        row.specJson(),
                        row.priceAmount(),
                        row.costPriceAmount(),
                        row.manualStockTotal(),
                        row.active(),
                        row.sortOrder());
                kept.add(newId);
                byCode.put(ck, newId);
            }
        }
        for (Map<String, Object> row : existing) {
            long id = ((Number) row.get("id")).longValue();
            if (!kept.contains(id)) {
                jdbc.update(
                        "DELETE FROM product_skus WHERE id = :id AND product_id = :pid",
                        new MapSqlParameterSource("id", id).addValue("pid", productId));
            }
        }
    }

    private long insertSku(
            long productId,
            String skuCode,
            String specJson,
            BigDecimal price,
            BigDecimal cost,
            int manual,
            boolean active,
            int sort) {
        String sql =
                """
                INSERT INTO product_skus (product_id, sku_code, spec_values, price_amount, cost_price_amount,
                manual_stock_total, manual_stock_locked, manual_stock_sold, is_active, sort_order, created_at, updated_at)
                VALUES (:pid, :code, CAST(:spec AS jsonb), :price, :cost, :manual, 0, 0, :active, :sort, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("pid", productId)
                        .addValue("code", skuCode)
                        .addValue("spec", specJson)
                        .addValue("price", price)
                        .addValue("cost", cost)
                        .addValue("manual", manual)
                        .addValue("active", active)
                        .addValue("sort", sort),
                kh,
                new String[] {"id"});
        Number key = kh.getKey();
        return key == null ? 0L : key.longValue();
    }
}
