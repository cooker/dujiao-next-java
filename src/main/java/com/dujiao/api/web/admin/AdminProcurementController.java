package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/procurement-orders")
public class AdminProcurementController {

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/{id}/upstream-payload/download")
    public ResponseEntity<?> downloadPayload(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retry(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
