package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.giftcard.GiftCardBatchStatusRequest;
import com.dujiao.api.dto.giftcard.GiftCardDto;
import com.dujiao.api.dto.giftcard.GiftCardGenerateRequest;
import com.dujiao.api.dto.giftcard.GiftCardGenerateResponse;
import com.dujiao.api.dto.giftcard.GiftCardUpdateRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminGiftCardService;
import com.dujiao.api.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/gift-cards")
public class AdminGiftCardController {

    private final AdminGiftCardService adminGiftCardService;

    public AdminGiftCardController(AdminGiftCardService adminGiftCardService) {
        this.adminGiftCardService = adminGiftCardService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GiftCardGenerateResponse>> generate(
            @Valid @RequestBody GiftCardGenerateRequest req) {
        long adminId = SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminGiftCardService.generate(req, adminId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GiftCardDto>>> list() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminGiftCardService.list()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GiftCardDto>> update(
            @PathVariable String id, @Valid @RequestBody GiftCardUpdateRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminGiftCardService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminGiftCardService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", true)));
    }

    @PatchMapping("/batch-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchStatus(
            @Valid @RequestBody GiftCardBatchStatusRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(Map.of("affected", adminGiftCardService.batchStatus(req))));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export() {
        SecurityUtils.requireAdminId();
        byte[] csv = adminGiftCardService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"gift-cards.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
