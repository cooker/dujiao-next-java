package com.dujiao.api.service;

import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.CardSecretBatchEntity;
import com.dujiao.api.domain.CardSecretEntity;
import com.dujiao.api.domain.Product;
import com.dujiao.api.domain.ProductSku;
import com.dujiao.api.dto.cardsecret.BatchDeleteCardSecretRequest;
import com.dujiao.api.dto.cardsecret.BatchUpdateCardSecretStatusRequest;
import com.dujiao.api.dto.cardsecret.CardSecretBatchSummaryDto;
import com.dujiao.api.dto.cardsecret.CardSecretDto;
import com.dujiao.api.dto.cardsecret.CardSecretQueryRequest;
import com.dujiao.api.dto.cardsecret.CardSecretStatsDto;
import com.dujiao.api.dto.cardsecret.ExportCardSecretRequest;
import com.dujiao.api.dto.cardsecret.UpdateCardSecretRequest;
import com.dujiao.api.repository.CardSecretBatchRepository;
import com.dujiao.api.repository.CardSecretRepository;
import com.dujiao.api.repository.CardSecretRepository.BatchStatusCountRow;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.repository.ProductSkuRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminCardSecretService {

    private static final String FULFILLMENT_AUTO = "auto";
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_CSV = "csv";
    private static final String EXPORT_TXT = "txt";
    private static final String EXPORT_CSV = "csv";

    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final CardSecretBatchRepository cardSecretBatchRepository;
    private final CardSecretRepository cardSecretRepository;

    public AdminCardSecretService(
            ProductRepository productRepository,
            ProductSkuRepository productSkuRepository,
            CardSecretBatchRepository cardSecretBatchRepository,
            CardSecretRepository cardSecretRepository) {
        this.productRepository = productRepository;
        this.productSkuRepository = productSkuRepository;
        this.cardSecretBatchRepository = cardSecretBatchRepository;
        this.cardSecretRepository = cardSecretRepository;
    }

    public record ListParams(
            long productId,
            long skuId,
            long batchId,
            String status,
            String secret,
            String batchNo,
            int page,
            int pageSize) {}

    @Transactional
    public Map<String, Object> createBatchManual(
            long adminId, long productId, long skuId, List<String> secrets, String batchNo, String note) {
        return createBatch(adminId, productId, skuId, secrets, batchNo, note, SOURCE_MANUAL);
    }

    @Transactional
    public Map<String, Object> importCsv(
            long adminId, long productId, long skuId, MultipartFile file, String batchNo, String note) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        try (InputStream in = file.getInputStream()) {
            List<String> secrets = CardSecretCsvParser.parse(in);
            return createBatch(adminId, productId, skuId, secrets, batchNo, note, SOURCE_CSV);
        } catch (IOException e) {
            throw new BusinessException(ResponseCodes.INTERNAL, "card_secret_import_failed");
        }
    }

    public PageResponse<List<CardSecretDto>> list(ListParams params) {
        if (params.skuId() > 0 && params.productId() == 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        if (params.productId() > 0 && params.skuId() > 0) {
            Product p =
                    productRepository
                            .findById(params.productId())
                            .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "product_not_found"));
            resolveCardSecretSku(p, params.skuId());
        }
        int[] pg = normalizePagination(params.page(), params.pageSize());
        var specFilter =
                new CardSecretSpecifications.ListFilter(
                        params.productId(),
                        params.skuId(),
                        params.batchId(),
                        params.status(),
                        params.secret(),
                        params.batchNo());
        Pageable pr =
                PageRequest.of(pg[0] - 1, pg[1], Sort.by(Sort.Direction.ASC, "id"));
        Page<CardSecretEntity> page =
                cardSecretRepository.findAll(CardSecretSpecifications.matches(specFilter), pr);
        List<CardSecretDto> list = page.getContent().stream().map(CardSecretDto::from).toList();
        return PageResponse.success(
                list, PaginationDto.of(pg[0], pg[1], page.getTotalElements()));
    }

    @Transactional
    public CardSecretDto update(long id, UpdateCardSecretRequest req) {
        if (id <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        CardSecretEntity item =
                cardSecretRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "card_secret_not_found"));
        String secret = req.secret() == null ? "" : req.secret().trim();
        String status = req.status() == null ? "" : req.status().trim();
        if (secret.isEmpty() && status.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        if (!secret.isEmpty()) {
            item.setSecret(secret);
        }
        if (!status.isEmpty()) {
            if (!status.equals(CardSecretEntity.STATUS_AVAILABLE)
                    && !status.equals(CardSecretEntity.STATUS_RESERVED)
                    && !status.equals(CardSecretEntity.STATUS_USED)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
            }
            item.setStatus(status);
        }
        item.setUpdatedAt(Instant.now());
        cardSecretRepository.save(item);
        return CardSecretDto.from(item);
    }

    @Transactional
    public Map<String, Object> batchUpdateStatus(BatchUpdateCardSecretStatusRequest req) {
        String st = req.status().trim();
        if (!st.equals(CardSecretEntity.STATUS_AVAILABLE)
                && !st.equals(CardSecretEntity.STATUS_RESERVED)
                && !st.equals(CardSecretEntity.STATUS_USED)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        long batchId = req.batchId() == null ? 0 : req.batchId();
        var filter = toListFilter(req.filter());
        List<Long> ids = resolveBatchTargetIds(req.ids(), batchId, filter);
        int n = cardSecretRepository.batchUpdateStatus(ids, st, Instant.now());
        return Map.of("affected", (long) n);
    }

    @Transactional
    public Map<String, Object> batchDelete(BatchDeleteCardSecretRequest req) {
        long batchId = req.batchId() == null ? 0 : req.batchId();
        var filter = toListFilter(req.filter());
        List<Long> ids = resolveBatchTargetIds(req.ids(), batchId, filter);
        int n = cardSecretRepository.softDeleteByIds(ids, Instant.now());
        return Map.of("affected", (long) n);
    }

    public ExportResult export(ExportCardSecretRequest req) {
        String fmt = req.format().trim().toLowerCase();
        if (!fmt.equals(EXPORT_TXT) && !fmt.equals(EXPORT_CSV)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        long batchId = req.batchId() == null ? 0 : req.batchId();
        var filter = toListFilter(req.filter());
        List<Long> ids = resolveBatchTargetIds(req.ids(), batchId, filter);
        List<CardSecretEntity> items = new ArrayList<>(cardSecretRepository.findAllById(ids));
        items.sort(Comparator.comparing(CardSecretEntity::getId));
        if (items.isEmpty()) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "card_secret_not_found");
        }
        if (fmt.equals(EXPORT_TXT)) {
            List<String> lines = new ArrayList<>();
            for (CardSecretEntity e : items) {
                String s = e.getSecret() == null ? "" : e.getSecret().trim();
                if (!s.isEmpty()) {
                    lines.add(s);
                }
            }
            byte[] body = String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
            return new ExportResult(body, "text/plain; charset=utf-8");
        }
        try (StringWriter sw = new StringWriter();
                CSVPrinter w =
                        new CSVPrinter(
                                sw,
                                CSVFormat.DEFAULT.builder().build())) {
            w.printRecord(
                    "id",
                    "secret",
                    "status",
                    "product_id",
                    "sku_id",
                    "order_id",
                    "batch_id",
                    "created_at");
            for (CardSecretEntity item : items) {
                String orderId = item.getOrderId() == null ? "" : String.valueOf(item.getOrderId());
                String bid = item.getBatchId() == null ? "" : String.valueOf(item.getBatchId());
                w.printRecord(
                        String.valueOf(item.getId()),
                        item.getSecret(),
                        item.getStatus(),
                        String.valueOf(item.getProductId()),
                        String.valueOf(item.getSkuId()),
                        orderId,
                        bid,
                        item.getCreatedAt() == null ? "" : item.getCreatedAt().toString());
            }
            w.flush();
            return new ExportResult(sw.toString().getBytes(StandardCharsets.UTF_8), "text/csv; charset=utf-8");
        } catch (IOException e) {
            throw new BusinessException(ResponseCodes.INTERNAL, "card_secret_fetch_failed");
        }
    }

    public CardSecretStatsDto stats(long productId, long skuId) {
        if (productId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        Product p =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "product_not_found"));
        if (skuId > 0) {
            resolveCardSecretSku(p, skuId);
        }
        long total = cardSecretRepository.countTotalForProduct(productId, skuId);
        long available =
                cardSecretRepository.countByProductAndSkuAndStatus(
                        productId, skuId, CardSecretEntity.STATUS_AVAILABLE);
        long used =
                cardSecretRepository.countByProductAndSkuAndStatus(
                        productId, skuId, CardSecretEntity.STATUS_USED);
        long reserved =
                cardSecretRepository.countByProductAndSkuAndStatus(
                        productId, skuId, CardSecretEntity.STATUS_RESERVED);
        return new CardSecretStatsDto(total, available, reserved, used);
    }

    public PageResponse<List<CardSecretBatchSummaryDto>> listBatches(
            long productId, long skuId, int page, int pageSize) {
        if (productId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        Product p =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "product_not_found"));
        if (skuId > 0) {
            resolveCardSecretSku(p, skuId);
        }
        int[] pg = normalizePagination(page, pageSize);
        Pageable pr =
                PageRequest.of(pg[0] - 1, pg[1], Sort.by(Sort.Direction.DESC, "id"));
        Page<CardSecretBatchEntity> pageResult =
                cardSecretBatchRepository.pageByProduct(productId, skuId, pr);
        List<CardSecretBatchEntity> rows = pageResult.getContent();
        if (rows.isEmpty()) {
            return PageResponse.success(
                    List.of(), PaginationDto.of(pg[0], pg[1], pageResult.getTotalElements()));
        }
        List<Long> batchIds = rows.stream().map(CardSecretBatchEntity::getId).toList();
        List<BatchStatusCountRow> countRows =
                cardSecretRepository.countGroupedByBatchAndStatus(batchIds);
        Map<Long, SkuCounts> map = new HashMap<>();
        for (BatchStatusCountRow row : countRows) {
            SkuCounts c = map.computeIfAbsent(row.getBatchId(), k -> new SkuCounts());
            long t = row.getTotal();
            switch (row.getStatus()) {
                case CardSecretEntity.STATUS_AVAILABLE -> c.available = t;
                case CardSecretEntity.STATUS_RESERVED -> c.reserved = t;
                case CardSecretEntity.STATUS_USED -> c.used = t;
                default -> {
                    /* ignore */
                }
            }
        }
        List<CardSecretBatchSummaryDto> out = new ArrayList<>();
        for (CardSecretBatchEntity item : rows) {
            SkuCounts c = map.getOrDefault(item.getId(), new SkuCounts());
            long tc = c.available + c.reserved + c.used;
            out.add(
                    new CardSecretBatchSummaryDto(
                            item.getId(),
                            item.getProductId(),
                            item.getSkuId(),
                            "",
                            item.getBatchNo(),
                            item.getSource(),
                            item.getNote() == null ? "" : item.getNote(),
                            tc,
                            c.available,
                            c.reserved,
                            c.used,
                            item.getCreatedAt()));
        }
        return PageResponse.success(
                out, PaginationDto.of(pg[0], pg[1], pageResult.getTotalElements()));
    }

    public record ExportResult(byte[] body, String contentType) {}

    private static final class SkuCounts {
        long available;
        long reserved;
        long used;
    }

    private Map<String, Object> createBatch(
            long adminId,
            long productId,
            long skuId,
            List<String> rawSecrets,
            String batchNo,
            String note,
            String source) {
        if (productId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "product_not_found"));
        ProductSku sku = resolveCardSecretSku(product, skuId);
        List<String> normalized = CardSecretCsvParser.normalizeSecrets(rawSecrets);
        if (normalized.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }

        String bn =
                batchNo == null || batchNo.isBlank()
                        ? generateBatchNo()
                        : batchNo.trim();
        String src = source == null || source.isBlank() ? SOURCE_MANUAL : source.trim();
        String n = note == null ? "" : note.trim();
        Instant now = Instant.now();

        CardSecretBatchEntity batch = new CardSecretBatchEntity();
        batch.setProductId(productId);
        batch.setSkuId(sku.getId());
        batch.setBatchNo(bn);
        batch.setSource(src);
        batch.setTotalCount(normalized.size());
        batch.setNote(n);
        if (adminId > 0) {
            batch.setCreatedBy(adminId);
        }
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        try {
            batch = cardSecretBatchRepository.save(batch);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ResponseCodes.INTERNAL, "card_secret_batch_create_failed");
        }

        List<CardSecretEntity> items = new ArrayList<>(normalized.size());
        for (String secret : normalized) {
            CardSecretEntity row = new CardSecretEntity();
            row.setProductId(productId);
            row.setSkuId(sku.getId());
            row.setBatch(cardSecretBatchRepository.getReferenceById(batch.getId()));
            row.setSecret(secret);
            row.setStatus(CardSecretEntity.STATUS_AVAILABLE);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            items.add(row);
        }
        final int chunk = 200;
        for (int i = 0; i < items.size(); i += chunk) {
            int end = Math.min(i + chunk, items.size());
            cardSecretRepository.saveAll(items.subList(i, end));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("created", batch.getTotalCount());
        body.put("batch_id", batch.getId());
        body.put("batch_no", batch.getBatchNo());
        return body;
    }

    private ProductSku resolveCardSecretSku(Product product, long rawSkuId) {
        long productId = product.getId();
        List<ProductSku> skus =
                productSkuRepository.findByProductIdAndDeletedAtIsNullOrderBySortOrderDescIdAsc(productId);
        List<ProductSku> activeSkus = skus.stream().filter(ProductSku::isActive).toList();

        if (rawSkuId > 0) {
            ProductSku sku =
                    productSkuRepository
                            .findByIdAndProductIdAndDeletedAtIsNull(rawSkuId, productId)
                            .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid"));
            if (FULFILLMENT_AUTO.equalsIgnoreCase(safeTrim(product.getFulfillmentType())) && !sku.isActive()) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
            }
            return sku;
        }

        if (FULFILLMENT_AUTO.equalsIgnoreCase(safeTrim(product.getFulfillmentType()))) {
            if (activeSkus.size() == 1) {
                return activeSkus.getFirst();
            }
            if (activeSkus.size() > 1) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
            }
        }

        return productSkuRepository
                .findByProductIdAndSkuCodeAndDeletedAtIsNull(productId, AdminProductSkuJdbcSync.DEFAULT_SKU_CODE)
                .or(() -> skus.size() == 1 ? java.util.Optional.of(skus.getFirst()) : java.util.Optional.empty())
                .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid"));
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String generateBatchNo() {
        String ts =
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.now());
        int n = ThreadLocalRandom.current().nextInt(10_000);
        return String.format("BATCH-%s-%04d", ts, n);
    }

    private static int[] normalizePagination(int page, int pageSize) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize <= 0) {
            pageSize = 20;
        }
        if (pageSize > 200) {
            pageSize = 200;
        }
        return new int[] {page, pageSize};
    }

    private CardSecretSpecifications.ListFilter toListFilter(CardSecretQueryRequest q) {
        if (q == null) {
            return new CardSecretSpecifications.ListFilter(0, 0, 0, "", "", "");
        }
        long pid = q.productId() == null ? 0 : q.productId();
        long sid = q.skuId() == null ? 0 : q.skuId();
        long bid = q.batchId() == null ? 0 : q.batchId();
        String st = q.status() == null ? "" : q.status();
        String sec = q.secret() == null ? "" : q.secret();
        String bn = q.batchNo() == null ? "" : q.batchNo();
        return new CardSecretSpecifications.ListFilter(pid, sid, bid, st, sec, bn);
    }

    private boolean hasListFilter(CardSecretSpecifications.ListFilter f) {
        return f.productId() > 0
                || f.skuId() > 0
                || f.batchId() > 0
                || (f.status() != null && !f.status().isBlank())
                || (f.secret() != null && !f.secret().isBlank())
                || (f.batchNo() != null && !f.batchNo().isBlank());
    }

    private List<Long> resolveBatchTargetIds(
            List<Long> ids, long batchId, CardSecretSpecifications.ListFilter filter) {
        List<Long> normalized = normalizeIds(ids);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        if (hasListFilter(filter)) {
            List<Long> targetIds =
                    cardSecretRepository
                            .findAll(
                                    CardSecretSpecifications.matches(filter),
                                    Sort.by(Sort.Direction.ASC, "id"))
                            .stream()
                            .map(CardSecretEntity::getId)
                            .toList();
            if (targetIds.isEmpty()) {
                throw new BusinessException(ResponseCodes.NOT_FOUND, "card_secret_not_found");
            }
            return targetIds;
        }
        if (batchId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "card_secret_invalid");
        }
        List<Long> targetIds = cardSecretRepository.listIdsByBatchId(batchId);
        if (targetIds.isEmpty()) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "card_secret_not_found");
        }
        return targetIds;
    }

    private static List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                set.add(id);
            }
        }
        return new ArrayList<>(set);
    }
}
