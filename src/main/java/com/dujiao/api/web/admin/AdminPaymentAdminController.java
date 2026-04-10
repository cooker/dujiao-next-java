package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.dto.payment.AdminPaymentRecordDto;
import com.dujiao.api.dto.payment.PaymentChannelDto;
import com.dujiao.api.dto.payment.PaymentChannelUpsertRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.admin.AdminPaymentChannelService;
import com.dujiao.api.service.admin.AdminPaymentRecordService;
import com.dujiao.api.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
public class AdminPaymentAdminController {

    private final AdminPaymentChannelService adminPaymentChannelService;
    private final AdminPaymentRecordService adminPaymentRecordService;

    public AdminPaymentAdminController(
            AdminPaymentChannelService adminPaymentChannelService,
            AdminPaymentRecordService adminPaymentRecordService) {
        this.adminPaymentChannelService = adminPaymentChannelService;
        this.adminPaymentRecordService = adminPaymentRecordService;
    }

    @PostMapping("/payment-channels")
    public ResponseEntity<ApiResponse<PaymentChannelDto>> createChannel(
            @Valid @RequestBody PaymentChannelUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminPaymentChannelService.create(req)));
    }

    @GetMapping("/payment-channels")
    public ResponseEntity<ApiResponse<List<PaymentChannelDto>>> listChannels() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminPaymentChannelService.list()));
    }

    @GetMapping("/payment-channels/{id}")
    public ResponseEntity<ApiResponse<PaymentChannelDto>> getChannel(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminPaymentChannelService.get(Long.parseLong(id))));
    }

    @PutMapping("/payment-channels/{id}")
    public ResponseEntity<ApiResponse<PaymentChannelDto>> updateChannel(
            @PathVariable String id, @Valid @RequestBody PaymentChannelUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminPaymentChannelService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/payment-channels/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminPaymentChannelService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/payments")
    public ResponseEntity<PageResponse<List<AdminPaymentRecordDto>>> listPayments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(adminPaymentRecordService.list(page, pageSize));
    }

    @GetMapping("/payments/export")
    public ResponseEntity<byte[]> exportPayments() {
        SecurityUtils.requireAdminId();
        byte[] csv = adminPaymentRecordService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-payments.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<AdminPaymentRecordDto>> getPayment(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminPaymentRecordService.get(Long.parseLong(id))));
    }
}
