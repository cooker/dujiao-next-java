package com.dujiao.api.service;

import com.dujiao.api.domain.CardSecretEntity;
import com.dujiao.api.domain.Product;
import com.dujiao.api.dto.category.CategoryDto;
import com.dujiao.api.dto.product.ProductDto;
import com.dujiao.api.dto.product.ProductSkuDto;
import com.dujiao.api.repository.CardSecretRepository;
import com.dujiao.api.repository.CardSecretRepository.CardStockAggRow;
import com.dujiao.api.util.LocalizedTitleJson;
import com.dujiao.api.util.PaymentChannelIdsJson;
import com.dujiao.api.util.ProductImagesJson;
import com.dujiao.api.util.ProductTagsJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 与 Go {@code decoratePublicProduct} / {@code ApplyAutoStockCounts} / {@code decorateProductStock}
 * 对齐的公开商品 JSON 装配（不含促销与会员价）。
 */
@Component
public class PublicProductAssembly {

    private static final int PUBLIC_LOW_STOCK_LIMIT = 5;
    private static final int MANUAL_UNLIMITED = -1;
    private static final String F_MANUAL = "manual";
    private static final String F_AUTO = "auto";
    private static final String F_UPSTREAM = "upstream";
    private static final String ST_UNLIMITED = "unlimited";
    private static final String ST_IN_STOCK = "in_stock";
    private static final String ST_LOW = "low_stock";
    private static final String ST_OUT = "out_of_stock";

    private static final String CS_AVAIL = CardSecretEntity.STATUS_AVAILABLE;
    private static final String CS_USED = CardSecretEntity.STATUS_USED;

    private final AdminProductSkuJdbcSync skuSync;
    private final CardSecretRepository cardSecretRepository;
    private final ObjectMapper objectMapper;

    public PublicProductAssembly(
            AdminProductSkuJdbcSync skuSync,
            CardSecretRepository cardSecretRepository,
            ObjectMapper objectMapper) {
        this.skuSync = skuSync;
        this.cardSecretRepository = cardSecretRepository;
        this.objectMapper = objectMapper;
    }

    public ProductDto buildPublicProductDto(Product p, CategoryDto categoryDto) {
        List<ProductSkuDto> raw = skuSync.listSkus(p.getId());
        List<ProductSkuDto> withAuto =
                "auto".equalsIgnoreCase(safeFt(p.getFulfillmentType()))
                        ? applyAutoStockCounts(p.getId(), raw)
                        : raw;
        List<ProductSkuDto> active =
                withAuto.stream().filter(ProductSkuDto::active).toList();
        BigDecimal displayPrice = resolveDisplayPrice(p, active);
        StockState st = resolveStockState(p, withAuto, active);

        return new ProductDto(
                p.getId(),
                p.getCategoryId(),
                p.getSlug(),
                readJsonObject(p.getSeoMetaJson()),
                LocalizedTitleJson.storedToResponseNode(p.getTitle()),
                readJsonObject(p.getDescriptionJson()),
                readJsonObject(p.getContentJson()),
                displayPrice,
                p.getCostPriceAmount() == null ? BigDecimal.ZERO : p.getCostPriceAmount(),
                ProductImagesJson.parse(p.getImagesJson(), objectMapper),
                ProductTagsJson.parse(p.getTagsJson(), objectMapper),
                p.getPurchaseType() == null ? "member" : p.getPurchaseType(),
                p.getMaxPurchaseQuantity(),
                p.getFulfillmentType(),
                readJsonObject(p.getManualFormSchemaJson()),
                p.getManualStockTotal(),
                p.getManualStockLocked(),
                p.getManualStockSold(),
                PaymentChannelIdsJson.decode(p.getPaymentChannelIds(), objectMapper),
                p.isMapped(),
                p.isActive(),
                p.getSortOrder(),
                p.isAffiliateEnabled(),
                categoryDto,
                active.isEmpty() ? null : active,
                st.manualAvailable(),
                st.autoAvailable(),
                st.stockStatus(),
                st.soldOut());
    }

    private JsonNode readJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private static String safeFt(String ft) {
        return ft == null ? "" : ft.trim();
    }

    private static BigDecimal resolveDisplayPrice(Product p, List<ProductSkuDto> activeSkus) {
        if (!activeSkus.isEmpty() && activeSkus.getFirst().priceAmount() != null) {
            return activeSkus.getFirst().priceAmount();
        }
        return p.getPriceAmount() == null ? BigDecimal.ZERO : p.getPriceAmount();
    }

    private List<ProductSkuDto> applyAutoStockCounts(long productId, List<ProductSkuDto> skus) {
        if (skus.isEmpty()) {
            return skus;
        }
        List<CardStockAggRow> rows =
                cardSecretRepository.countStockGroupedByProductSkuStatus(List.of(productId));
        Map<Long, Map<String, Long>> bySku = new HashMap<>();
        for (CardStockAggRow r : rows) {
            if (r.getProductId() != productId) {
                continue;
            }
            bySku
                    .computeIfAbsent(r.getSkuId(), k -> new HashMap<>())
                    .merge(r.getStatus(), r.getTotal(), Long::sum);
        }
        Map<String, Long> legacy = bySku.remove(0L);
        int legacyTarget = resolveLegacyTargetIndex(skus);
        List<ProductSkuDto> out = new ArrayList<>();
        for (int i = 0; i < skus.size(); i++) {
            ProductSkuDto s = skus.get(i);
            Map<String, Long> sm = new HashMap<>(bySku.getOrDefault(s.id(), Map.of()));
            if (i == legacyTarget && legacy != null) {
                for (var e : legacy.entrySet()) {
                    sm.merge(e.getKey(), e.getValue(), Long::sum);
                }
            }
            long av = sm.getOrDefault(CS_AVAIL, 0L);
            long us = sm.getOrDefault(CS_USED, 0L);
            long autoAvail = av;
            out.add(
                    new ProductSkuDto(
                            s.id(),
                            s.productId(),
                            s.skuCode(),
                            s.specValues(),
                            s.priceAmount(),
                            s.costPriceAmount(),
                            s.manualStockTotal(),
                            (int) us,
                            autoAvail,
                            0,
                            s.active(),
                            s.sortOrder()));
        }
        return out;
    }

    private static int resolveLegacyTargetIndex(List<ProductSkuDto> skus) {
        if (skus.isEmpty()) {
            return -1;
        }
        String def = AdminProductSkuJdbcSync.DEFAULT_SKU_CODE.toUpperCase(Locale.ROOT);
        int firstActive = -1;
        for (int i = 0; i < skus.size(); i++) {
            if (!skus.get(i).active()) {
                continue;
            }
            if (firstActive < 0) {
                firstActive = i;
            }
            String code = skus.get(i).skuCode() == null ? "" : skus.get(i).skuCode().trim();
            if (def.equalsIgnoreCase(code)) {
                return i;
            }
        }
        return firstActive >= 0 ? firstActive : 0;
    }

    private StockState resolveStockState(
            Product p, List<ProductSkuDto> allSkus, List<ProductSkuDto> activeSkus) {
        String ft = safeFt(p.getFulfillmentType());
        if (F_UPSTREAM.equalsIgnoreCase(ft)) {
            return new StockState(0, 0L, ST_IN_STOCK, false);
        }
        if (F_AUTO.equalsIgnoreCase(ft)) {
            long autoAvail = 0;
            for (ProductSkuDto s : allSkus) {
                if (!s.active()) {
                    continue;
                }
                autoAvail += s.autoStockAvailable();
            }
            if (autoAvail <= 0) {
                return new StockState(0, autoAvail, ST_OUT, true);
            }
            if (autoAvail <= PUBLIC_LOW_STOCK_LIMIT) {
                return new StockState(0, autoAvail, ST_LOW, false);
            }
            return new StockState(0, autoAvail, ST_IN_STOCK, false);
        }
        if (F_MANUAL.equalsIgnoreCase(ft) || ft.isEmpty()) {
            if (!activeSkus.isEmpty()) {
                boolean unlimited =
                        activeSkus.stream()
                                .anyMatch(s -> s.manualStockTotal() == MANUAL_UNLIMITED);
                if (unlimited) {
                    return new StockState(MANUAL_UNLIMITED, 0L, ST_UNLIMITED, false);
                }
                int sum = 0;
                for (ProductSkuDto s : activeSkus) {
                    if (s.manualStockTotal() != MANUAL_UNLIMITED && s.manualStockTotal() > 0) {
                        sum += s.manualStockTotal();
                    }
                }
                return stockFromManual(sum);
            }
            if (p.getManualStockTotal() == MANUAL_UNLIMITED) {
                return new StockState(MANUAL_UNLIMITED, 0L, ST_UNLIMITED, false);
            }
            int m = Math.max(0, p.getManualStockTotal());
            return stockFromManual(m);
        }
        return new StockState(0, 0L, ST_IN_STOCK, false);
    }

    private static StockState stockFromManual(int manualAvailable) {
        if (manualAvailable <= 0) {
            return new StockState(0, 0L, ST_OUT, true);
        }
        if (manualAvailable <= PUBLIC_LOW_STOCK_LIMIT) {
            return new StockState(manualAvailable, 0L, ST_LOW, false);
        }
        return new StockState(manualAvailable, 0L, ST_IN_STOCK, false);
    }

    private record StockState(
            Integer manualAvailable, Long autoAvailable, String stockStatus, boolean soldOut) {}
}
