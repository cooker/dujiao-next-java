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
@RequestMapping(ApiPaths.V1 + "/admin/site-connections")
public class AdminSiteConnectionController {

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping
    public ResponseEntity<?> create() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/{id}/ping")
    public ResponseEntity<?> ping(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/{id}/reapply-markup")
    public ResponseEntity<?> reapplyMarkup(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
