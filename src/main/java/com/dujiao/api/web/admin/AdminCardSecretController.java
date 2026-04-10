package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.dto.cardsecret.BatchDeleteCardSecretRequest;
import com.dujiao.api.dto.cardsecret.BatchUpdateCardSecretStatusRequest;
import com.dujiao.api.dto.cardsecret.CardSecretBatchCreateRequest;
import com.dujiao.api.dto.cardsecret.CardSecretBatchSummaryDto;
import com.dujiao.api.dto.cardsecret.CardSecretDto;
import com.dujiao.api.dto.cardsecret.CardSecretStatsDto;
import com.dujiao.api.dto.cardsecret.ExportCardSecretRequest;
import com.dujiao.api.dto.cardsecret.UpdateCardSecretRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminCardSecretService;
import com.dujiao.api.service.AdminCardSecretService.ExportResult;
import com.dujiao.api.service.AdminCardSecretService.ListParams;
import com.dujiao.api.web.ApiPaths;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/card-secrets")
public class AdminCardSecretController {

    private final AdminCardSecretService adminCardSecretService;

    public AdminCardSecretController(AdminCardSecretService adminCardSecretService) {
        this.adminCardSecretService = adminCardSecretService;
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBatch(
            @Valid @RequestBody CardSecretBatchCreateRequest req) {
        long adminId = SecurityUtils.requireAdminId();
        long skuId = req.skuId() == null ? 0L : req.skuId();
        return ResponseEntity.ok(
                ApiResponse.success(
                        adminCardSecretService.createBatchManual(
                                adminId, req.productId(), skuId, req.secrets(), req.batchNo(), req.note())));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> importCsv(
            @RequestParam("product_id") long productId,
            @RequestParam(value = "sku_id", defaultValue = "0") long skuId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "batch_no", required = false) String batchNo,
            @RequestParam(value = "note", required = false) String note) {
        long adminId = SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(
                        adminCardSecretService.importCsv(adminId, productId, skuId, file, batchNo, note)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<List<CardSecretDto>>> list(
            @RequestParam(name = "product_id", required = false) Long productId,
            @RequestParam(name = "sku_id", required = false) Long skuId,
            @RequestParam(name = "batch_id", required = false) Long batchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String secret,
            @RequestParam(name = "batch_no", required = false) String batchNo,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        SecurityUtils.requireAdminId();
        long pid = productId == null ? 0L : productId;
        long sid = skuId == null ? 0L : skuId;
        long bid = batchId == null ? 0L : batchId;
        return ResponseEntity.ok(
                adminCardSecretService.list(
                        new ListParams(pid, sid, bid, status, secret, batchNo, page, pageSize)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CardSecretDto>> update(
            @PathVariable String id, @Valid @RequestBody UpdateCardSecretRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminCardSecretService.update(Long.parseLong(id), req)));
    }

    @PatchMapping("/batch-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchStatus(
            @Valid @RequestBody BatchUpdateCardSecretStatusRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminCardSecretService.batchUpdateStatus(req)));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchDelete(
            @Valid @RequestBody BatchDeleteCardSecretRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminCardSecretService.batchDelete(req)));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@Valid @RequestBody ExportCardSecretRequest req) {
        SecurityUtils.requireAdminId();
        ExportResult r = adminCardSecretService.export(req);
        String fmt = req.format().trim().toLowerCase();
        String ts =
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                        .format(ZonedDateTime.now(ZoneId.systemDefault()));
        String filename = "card-secrets-" + ts + "." + fmt;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, r.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(r.body());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CardSecretStatsDto>> stats(
            @RequestParam(name = "product_id") long productId,
            @RequestParam(name = "sku_id", defaultValue = "0") long skuId) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminCardSecretService.stats(productId, skuId)));
    }

    @GetMapping("/batches")
    public ResponseEntity<PageResponse<List<CardSecretBatchSummaryDto>>> batches(
            @RequestParam(name = "product_id") long productId,
            @RequestParam(name = "sku_id", defaultValue = "0") long skuId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                adminCardSecretService.listBatches(productId, skuId, page, pageSize));
    }

    @GetMapping(value = "/template", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> template() {
        SecurityUtils.requireAdminId();
        String content = "secret\nCARD-AAA-0001\nCARD-BBB-0002\n";
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"card-secrets-template.csv\"")
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
