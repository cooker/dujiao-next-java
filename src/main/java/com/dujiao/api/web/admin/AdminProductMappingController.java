package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
public class AdminProductMappingController {

    @GetMapping("/product-mappings")
    public ResponseEntity<?> listMappings() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/product-mappings/{id}")
    public ResponseEntity<?> getMapping(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/product-mappings/import")
    public ResponseEntity<?> importProduct() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/product-mappings/batch-import")
    public ResponseEntity<?> batchImport() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/product-mappings/{id}/sync")
    public ResponseEntity<?> sync(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PutMapping("/product-mappings/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @DeleteMapping("/product-mappings/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/product-mappings/batch-sync")
    public ResponseEntity<?> batchSync() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/product-mappings/batch-status")
    public ResponseEntity<?> batchStatus() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/product-mappings/batch-delete")
    public ResponseEntity<?> batchDelete() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/upstream-products")
    public ResponseEntity<?> upstreamProducts() {
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
