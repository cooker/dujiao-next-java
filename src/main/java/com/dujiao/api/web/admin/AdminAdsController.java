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
@RequestMapping(ApiPaths.V1 + "/admin/ads")
public class AdminAdsController {

    @GetMapping("/render/{slotCode}")
    public ResponseEntity<?> render(@PathVariable String slotCode) {
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PostMapping("/impression")
    public ResponseEntity<?> impression() {
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
