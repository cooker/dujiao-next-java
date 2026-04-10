package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.Category;
import com.dujiao.api.domain.Product;
import com.dujiao.api.dto.product.ProductBatchCategoryRequest;
import com.dujiao.api.dto.product.ProductBatchIdsRequest;
import com.dujiao.api.dto.product.ProductBatchStatusRequest;
import com.dujiao.api.dto.product.ProductDto;
import com.dujiao.api.dto.product.ProductQuickUpdateRequest;
import com.dujiao.api.dto.product.ProductSkuDto;
import com.dujiao.api.dto.product.ProductUpsertRequest;
import com.dujiao.api.repository.CategoryRepository;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.util.CategoryDtoMapper;
import com.dujiao.api.util.JsonNodeText;
import com.dujiao.api.util.LocalizedTitleJson;
import com.dujiao.api.util.PaymentChannelIdsJson;
import com.dujiao.api.util.ProductImagesJson;
import com.dujiao.api.util.ProductTagsJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AdminProductSkuJdbcSync skuSync;

    public AdminProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper,
            AdminProductSkuJdbcSync skuSync) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.skuSync = skuSync;
    }

    private record PricingBundle(
            BigDecimal price,
            BigDecimal cost,
            int manualStock,
            boolean multiSku,
            List<AdminProductSkuJdbcSync.NormalizedSkuRow> skuRows) {}

    @Transactional(readOnly = true)
    public Page<ProductDto> listAdmin(
            String categoryId,
            String search,
            String fulfillmentType,
            String stockStatus,
            int lowStockThreshold,
            int page,
            int pageSize) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        String normalizedStockStatus = normalizeStockStatus(stockStatus);
        Specification<Product> spec =
                (root, query, cb) -> {
                    var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                    if (categoryId != null && !categoryId.isBlank()) {
                        try {
                            long cid = Long.parseLong(categoryId.trim());
                            predicates.add(cb.equal(root.get("categoryId"), cid));
                        } catch (NumberFormatException ignored) {
                            predicates.add(cb.equal(root.get("categoryId"), -1L));
                        }
                    }
                    if (search != null && !search.isBlank()) {
                        String kw = "%" + search.trim().toLowerCase() + "%";
                        predicates.add(
                                cb.or(
                                        cb.like(cb.lower(root.get("slug")), kw),
                                        cb.like(cb.lower(root.get("title")), kw)));
                    }
                    String normalizedFulfillment = normalizeFulfillmentTypeFilter(fulfillmentType);
                    if (!normalizedFulfillment.isEmpty()) {
                        predicates.add(cb.equal(root.get("fulfillmentType"), normalizedFulfillment));
                    }
                    return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };
        if (normalizedStockStatus.isEmpty()) {
            Page<Product> pg =
                    productRepository.findAll(
                            spec,
                            PageRequest.of(
                                    p - 1, ps, org.springframework.data.domain.Sort.by("id").descending()));
            Map<Long, Category> cats = loadCategoriesForProducts(pg.getContent());
            return pg.map(pr -> toDto(pr, cats, null));
        }
        List<Product> all =
                productRepository.findAll(spec, org.springframework.data.domain.Sort.by("id").descending());
        if (all.isEmpty()) {
            return Page.empty(PageRequest.of(p - 1, ps));
        }
        List<Product> filtered = filterByStockStatus(all, normalizedStockStatus, Math.max(lowStockThreshold, 0));
        int from = Math.min((p - 1) * ps, filtered.size());
        int to = Math.min(from + ps, filtered.size());
        List<Product> slice = filtered.subList(from, to);
        Map<Long, Category> cats = loadCategoriesForProducts(slice);
        List<ProductDto> content = slice.stream().map(pr -> toDto(pr, cats, null)).toList();
        return new PageImpl<>(content, PageRequest.of(p - 1, ps), filtered.size());
    }

    @Transactional(readOnly = true)
    public ProductDto get(long id) {
        Product p =
                productRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "product_not_found"));
        return toDto(p, null, skuSync.listSkus(id));
    }

    @Transactional
    public ProductDto create(ProductUpsertRequest req) {
        ensureCategory(req.categoryId());
        productRepository
                .findBySlug(req.slug().trim())
                .ifPresent(
                        x -> {
                            throw new BusinessException(ResponseCodes.BAD_REQUEST, "slug_exists");
                        });
        String purchaseType = resolvePurchaseType(req.purchaseType(), null);
        String fulfillment = resolveFulfillmentType(req.fulfillmentType(), null);
        PricingBundle pricing = resolvePricing(req, null, fulfillment);
        Product p = new Product();
        applyProductFields(p, req, pricing, true, purchaseType, fulfillment);
        Product saved = productRepository.save(p);
        afterSaveSyncSkus(saved.getId(), pricing);
        return toDto(saved, null, skuSync.listSkus(saved.getId()));
    }

    @Transactional
    public ProductDto update(long id, ProductUpsertRequest req) {
        Product p =
                productRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "product_not_found"));
        ensureCategory(req.categoryId());
        productRepository
                .findBySlug(req.slug().trim())
                .filter(x -> !x.getId().equals(id))
                .ifPresent(x -> {
                    throw new BusinessException(ResponseCodes.BAD_REQUEST, "slug_used");
                });
        String purchaseType = resolvePurchaseType(req.purchaseType(), p);
        String fulfillment = resolveFulfillmentType(req.fulfillmentType(), p);
        if (p.isMapped()) {
            fulfillment = "upstream";
        }
        PricingBundle pricing = resolvePricing(req, p, fulfillment);
        applyProductFields(p, req, pricing, false, purchaseType, fulfillment);
        Product saved = productRepository.save(p);
        afterSaveSyncSkus(saved.getId(), pricing);
        return toDto(saved, null, skuSync.listSkus(saved.getId()));
    }

    @Transactional
    public void delete(long id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "product_not_found");
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductDto quickUpdate(long id, ProductQuickUpdateRequest req) {
        Product p =
                productRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "product_not_found"));
        boolean touched = false;
        if (req.active() != null) {
            p.setActive(req.active());
            touched = true;
        }
        if (req.sortOrder() != null) {
            p.setSortOrder(req.sortOrder());
            touched = true;
        }
        if (req.categoryId() != null) {
            ensureCategory(req.categoryId());
            p.setCategoryId(req.categoryId());
            touched = true;
        }
        if (!touched) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        p.setUpdatedAt(Instant.now());
        return toDto(productRepository.save(p), null, null);
    }

    @Transactional
    public Map<String, Object> batchUpdateStatus(ProductBatchStatusRequest req) {
        int updated = 0;
        for (Long id : req.ids()) {
            var opt = productRepository.findById(id);
            if (opt.isPresent()) {
                Product p = opt.get();
                p.setActive(req.active());
                p.setUpdatedAt(Instant.now());
                productRepository.save(p);
                updated++;
            }
        }
        return Map.of("total", req.ids().size(), "success_count", updated);
    }

    @Transactional
    public Map<String, Object> batchUpdateCategory(ProductBatchCategoryRequest req) {
        ensureCategory(req.categoryId());
        int updated = 0;
        for (Long id : req.ids()) {
            var opt = productRepository.findById(id);
            if (opt.isPresent()) {
                Product p = opt.get();
                p.setCategoryId(req.categoryId());
                p.setUpdatedAt(Instant.now());
                productRepository.save(p);
                updated++;
            }
        }
        return Map.of("total", req.ids().size(), "success_count", updated);
    }

    @Transactional
    public Map<String, Object> batchDelete(ProductBatchIdsRequest req) {
        int deleted = 0;
        List<Long> failedIds = new java.util.ArrayList<>();
        for (Long id : req.ids()) {
            if (productRepository.existsById(id)) {
                productRepository.deleteById(id);
                deleted++;
            } else {
                failedIds.add(id);
            }
        }
        return Map.of("total", req.ids().size(), "success_count", deleted, "failed_ids", failedIds);
    }

    private void ensureCategory(long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "category_not_found");
        }
    }

    private String resolvePurchaseType(String raw, Product existing) {
        String r = raw == null ? "" : raw.trim();
        if (r.isEmpty() && existing != null) {
            r = existing.getPurchaseType() == null ? "" : existing.getPurchaseType();
        }
        r = r.toLowerCase();
        if (r.isEmpty() || "member".equals(r)) {
            return "member";
        }
        if ("guest".equals(r)) {
            return "guest";
        }
        throw new BusinessException(ResponseCodes.BAD_REQUEST, "product_purchase_invalid");
    }

    private String resolveFulfillmentType(String raw, Product existing) {
        String r = raw == null ? "" : raw.trim();
        if (r.isEmpty() && existing != null) {
            r = existing.getFulfillmentType() == null ? "" : existing.getFulfillmentType();
        }
        return normalizeFulfillmentType(r.isEmpty() ? null : r);
    }

    private PricingBundle resolvePricing(ProductUpsertRequest req, Product existing, String fulfillmentType) {
        boolean hasSkus = req.skus() != null && !req.skus().isEmpty();
        if (hasSkus) {
            Set<Long> existingIds =
                    existing == null ? Set.of() : skuSync.existingSkuIds(existing.getId());
            var agg = skuSync.normalizeSkuRequests(req.skus(), fulfillmentType, existingIds);
            return new PricingBundle(
                    agg.minActivePrice(), agg.minActiveCost(), agg.productManualStockTotal(), true, agg.rows());
        }
        BigDecimal price = req.priceAmount().setScale(2, RoundingMode.HALF_UP);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "product_price_invalid");
        }
        BigDecimal cost =
                req.costPriceAmount() == null
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : req.costPriceAmount().setScale(2, RoundingMode.HALF_UP);
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "product_price_invalid");
        }
        Integer m = req.manualStockTotal();
        int manual;
        if (m == null) {
            manual = existing == null ? 0 : existing.getManualStockTotal();
        } else {
            manual = normalizeManualStockTotal(m);
        }
        return new PricingBundle(price, cost, manual, false, null);
    }

    private void applyProductFields(
            Product p,
            ProductUpsertRequest req,
            PricingBundle pricing,
            boolean isCreate,
            String purchaseType,
            String fulfillment) {
        p.setCategoryId(req.categoryId());
        p.setSlug(req.slug().trim());
        p.setTitle(LocalizedTitleJson.requestToStoredJson(req.title()));
        p.setSeoMetaJson(JsonNodeText.toStored(objectMapper, req.seoMeta()));
        p.setDescriptionJson(JsonNodeText.toStored(objectMapper, req.description()));
        p.setContentJson(JsonNodeText.toStored(objectMapper, req.content()));
        if ("manual".equals(fulfillment)) {
            p.setManualFormSchemaJson(JsonNodeText.toStored(objectMapper, req.manualFormSchema()));
        } else {
            p.setManualFormSchemaJson("{}");
        }
        p.setPriceAmount(pricing.price());
        p.setCostPriceAmount(pricing.cost());
        p.setManualStockTotal(pricing.manualStock());
        p.setImagesJson(ProductImagesJson.toStoredJson(req.images(), objectMapper));
        p.setTagsJson(ProductTagsJson.toStored(req.tags(), objectMapper));
        p.setPurchaseType(purchaseType);
        if (isCreate || req.maxPurchaseQuantity() != null) {
            int mx = req.maxPurchaseQuantity() == null ? 0 : req.maxPurchaseQuantity();
            p.setMaxPurchaseQuantity(mx <= 0 ? 0 : mx);
        }
        p.setFulfillmentType(fulfillment);
        p.setPaymentChannelIds(PaymentChannelIdsJson.encode(req.paymentChannelIds(), objectMapper));
        if (isCreate) {
            p.setActive(req.active() == null || req.active());
            p.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
            p.setAffiliateEnabled(req.affiliateEnabled() != null && req.affiliateEnabled());
            p.setMapped(false);
            p.setManualStockLocked(0);
            p.setManualStockSold(0);
        } else {
            if (req.active() != null) {
                p.setActive(req.active());
            }
            p.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
            if (req.affiliateEnabled() != null) {
                p.setAffiliateEnabled(req.affiliateEnabled());
            }
        }
        p.setUpdatedAt(Instant.now());
    }

    private void afterSaveSyncSkus(long productId, PricingBundle pricing) {
        if (pricing.multiSku()) {
            skuSync.applyMultiSku(productId, pricing.skuRows());
        } else {
            skuSync.syncSingleSku(productId, pricing.price(), pricing.cost(), pricing.manualStock(), true);
        }
    }

    private Map<Long, Category> loadCategoriesForProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids =
                products.stream().map(Product::getCategoryId).collect(Collectors.toSet());
        return categoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    private ProductDto toDto(Product p, Map<Long, Category> categoryCache, List<ProductSkuDto> skus) {
        Category cat = resolveCategory(p, categoryCache);
        return new ProductDto(
                p.getId(),
                p.getCategoryId(),
                p.getSlug(),
                readJsonObject(p.getSeoMetaJson()),
                LocalizedTitleJson.storedToResponseNode(p.getTitle()),
                readJsonObject(p.getDescriptionJson()),
                readJsonObject(p.getContentJson()),
                p.getPriceAmount(),
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
                cat != null ? CategoryDtoMapper.from(cat, objectMapper) : null,
                skus == null || skus.isEmpty() ? null : skus,
                null,
                null,
                null,
                null);
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

    private Category resolveCategory(Product p, Map<Long, Category> categoryCache) {
        if (categoryCache != null) {
            Category c = categoryCache.get(p.getCategoryId());
            if (c != null) {
                return c;
            }
        }
        return categoryRepository.findById(p.getCategoryId()).orElse(null);
    }

    /** 查询筛选用：非法值不抛异常。 */
    private String normalizeFulfillmentTypeFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim().toLowerCase();
        if ("manual".equals(value) || "auto".equals(value) || "upstream".equals(value)) {
            return value;
        }
        return "";
    }

    private String normalizeFulfillmentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "manual";
        }
        String value = raw.trim().toLowerCase();
        if ("manual".equals(value) || "auto".equals(value) || "upstream".equals(value)) {
            return value;
        }
        throw new BusinessException(ResponseCodes.BAD_REQUEST, "fulfillment_invalid");
    }

    private int normalizeManualStockTotal(int value) {
        if (value < -1) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "manual_stock_invalid");
        }
        return value;
    }

    private String normalizeStockStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim().toLowerCase();
        if ("all".equals(value)) {
            return "";
        }
        if ("low".equals(value) || "normal".equals(value) || "unlimited".equals(value)) {
            return value;
        }
        return "";
    }

    private String safeFulfillmentRead(String raw) {
        if (raw == null || raw.isBlank()) {
            return "manual";
        }
        String value = raw.trim().toLowerCase();
        if ("manual".equals(value) || "auto".equals(value) || "upstream".equals(value)) {
            return value;
        }
        return "manual";
    }

    private List<Product> filterByStockStatus(List<Product> products, String status, int lowStockThreshold) {
        Set<Long> ids = products.stream().map(Product::getId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ManualStats> manualStats = loadManualStats(ids);
        Map<Long, Long> autoAvailable = loadAutoAvailable(ids);
        Map<Long, UpstreamStats> upstreamStats = loadUpstreamStats(ids);
        List<Product> result = new ArrayList<>();
        for (Product p : products) {
            String ft = safeFulfillmentRead(p.getFulfillmentType());
            if ("manual".equals(ft) && matchesManualStatus(p, status, manualStats.get(p.getId()), lowStockThreshold)) {
                result.add(p);
            } else if ("auto".equals(ft)
                    && matchesAutoStatus(status, autoAvailable.getOrDefault(p.getId(), 0L), lowStockThreshold)) {
                result.add(p);
            } else if ("upstream".equals(ft)
                    && matchesUpstreamStatus(
                            status, upstreamStats.getOrDefault(p.getId(), new UpstreamStats(false, 0L)), lowStockThreshold)) {
                result.add(p);
            }
        }
        return result;
    }

    private boolean matchesManualStatus(Product p, String status, ManualStats stats, int lowStockThreshold) {
        boolean hasActiveSku = stats != null && stats.activeCount > 0;
        boolean hasUnlimitedSku = stats != null && stats.unlimitedExists;
        long skuRemaining = stats == null ? 0 : stats.remainingSum;
        if ("unlimited".equals(status)) {
            return hasUnlimitedSku || (!hasActiveSku && p.getManualStockTotal() == -1);
        }
        if ("low".equals(status)) {
            return ((hasActiveSku && !hasUnlimitedSku && skuRemaining <= 0)
                    || (!hasActiveSku && p.getManualStockTotal() == 0));
        }
        if ("normal".equals(status)) {
            return ((hasActiveSku && !hasUnlimitedSku && skuRemaining > 0)
                    || (!hasActiveSku && p.getManualStockTotal() > 0));
        }
        return true;
    }

    private boolean matchesAutoStatus(String status, long available, int lowStockThreshold) {
        if ("low".equals(status)) {
            return available >= 0 && available <= lowStockThreshold;
        }
        if ("normal".equals(status)) {
            return available > lowStockThreshold;
        }
        return false;
    }

    private boolean matchesUpstreamStatus(String status, UpstreamStats stats, int lowStockThreshold) {
        if ("unlimited".equals(status)) {
            return stats.unlimitedExists;
        }
        if ("low".equals(status)) {
            return !stats.unlimitedExists && stats.stockSum == 0;
        }
        if ("normal".equals(status)) {
            return !stats.unlimitedExists && stats.stockSum > 0;
        }
        return false;
    }

    private Map<Long, ManualStats> loadManualStats(Set<Long> ids) {
        try {
            String sql =
                    """
                    SELECT ps.product_id AS product_id,
                           COUNT(*) AS active_count,
                           MAX(CASE WHEN ps.manual_stock_total = -1 THEN 1 ELSE 0 END) AS unlimited_exists,
                           COALESCE(SUM(CASE WHEN ps.manual_stock_total > 0 THEN ps.manual_stock_total ELSE 0 END), 0) AS remaining_sum
                    FROM product_skus ps
                    WHERE ps.product_id IN (:ids) AND ps.is_active = true AND ps.deleted_at IS NULL
                    GROUP BY ps.product_id
                    """;
            Map<Long, ManualStats> map = new HashMap<>();
            jdbc.query(
                    sql,
                    new MapSqlParameterSource("ids", ids),
                    rs -> {
                        long id = rs.getLong("product_id");
                        map.put(
                                id,
                                new ManualStats(
                                        rs.getLong("active_count"),
                                        rs.getInt("unlimited_exists") > 0,
                                        rs.getLong("remaining_sum")));
                    });
            return map;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private Map<Long, Long> loadAutoAvailable(Set<Long> ids) {
        try {
            String sql =
                    """
                    SELECT cs.product_id AS product_id, COUNT(*) AS available_count
                    FROM card_secrets cs
                    WHERE cs.product_id IN (:ids) AND cs.status = 'available' AND cs.deleted_at IS NULL
                    GROUP BY cs.product_id
                    """;
            Map<Long, Long> map = new HashMap<>();
            var rows = jdbc.queryForList(sql, new MapSqlParameterSource("ids", ids));
            for (var row : rows) {
                Object pid = row.get("product_id");
                Object available = row.get("available_count");
                if (pid instanceof Number pnum && available instanceof Number anum) {
                    map.put(pnum.longValue(), anum.longValue());
                }
            }
            return map;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private Map<Long, UpstreamStats> loadUpstreamStats(Set<Long> ids) {
        try {
            String sql =
                    """
                    SELECT pm.local_product_id AS product_id,
                           MAX(CASE WHEN sm.upstream_stock = -1 THEN 1 ELSE 0 END) AS unlimited_exists,
                           COALESCE(SUM(CASE WHEN sm.upstream_stock > 0 THEN sm.upstream_stock ELSE 0 END), 0) AS stock_sum
                    FROM product_mappings pm
                    JOIN sku_mappings sm ON sm.product_mapping_id = pm.id AND sm.deleted_at IS NULL
                    WHERE pm.local_product_id IN (:ids) AND pm.deleted_at IS NULL
                    GROUP BY pm.local_product_id
                    """;
            Map<Long, UpstreamStats> map = new HashMap<>();
            jdbc.query(
                    sql,
                    new MapSqlParameterSource("ids", ids),
                    rs -> {
                        long id = rs.getLong("product_id");
                        map.put(id, new UpstreamStats(rs.getInt("unlimited_exists") > 0, rs.getLong("stock_sum")));
                    });
            return map;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private record ManualStats(long activeCount, boolean unlimitedExists, long remainingSum) {}

    private record UpstreamStats(boolean unlimitedExists, long stockSum) {}
}
