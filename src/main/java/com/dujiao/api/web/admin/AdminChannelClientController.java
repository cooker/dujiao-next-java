package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.channel.ChannelClientCreateRequest;
import com.dujiao.api.dto.channel.ChannelClientCreateResponse;
import com.dujiao.api.dto.channel.ChannelClientDto;
import com.dujiao.api.dto.channel.ChannelClientSecretResetResponse;
import com.dujiao.api.dto.channel.ChannelClientStatusRequest;
import com.dujiao.api.dto.channel.ChannelClientUpdateRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminChannelClientService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/channel-clients")
public class AdminChannelClientController {

    private final AdminChannelClientService adminChannelClientService;

    public AdminChannelClientController(AdminChannelClientService adminChannelClientService) {
        this.adminChannelClientService = adminChannelClientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelClientDto>>> list() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminChannelClientService.list()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChannelClientCreateResponse>> create(
            @Valid @RequestBody ChannelClientCreateRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminChannelClientService.create(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelClientDto>> get(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminChannelClientService.get(Long.parseLong(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelClientDto>> update(
            @PathVariable String id, @Valid @RequestBody ChannelClientUpdateRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminChannelClientService.update(Long.parseLong(id), req)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ChannelClientDto>> updateStatus(
            @PathVariable String id, @Valid @RequestBody ChannelClientStatusRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(
                        adminChannelClientService.updateStatus(Long.parseLong(id), req)));
    }

    @PostMapping("/{id}/reset-secret")
    public ResponseEntity<ApiResponse<ChannelClientSecretResetResponse>> resetSecret(
            @PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(
                        adminChannelClientService.resetSecret(Long.parseLong(id))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminChannelClientService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
