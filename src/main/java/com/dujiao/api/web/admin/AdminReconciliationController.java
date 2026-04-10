package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/reconciliation")
public class AdminReconciliationController {

    @PostMapping("/run")
    public ResponseEntity<?> run() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> listJobs() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<?> getJob(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PutMapping("/items/{id}/resolve")
    public ResponseEntity<?> resolveItem(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
