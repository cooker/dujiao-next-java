package com.dujiao.api.web.upstream;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/upstream")
public class UpstreamApiController {

    @PostMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/products")
    public ResponseEntity<?> listProducts() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder() {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
