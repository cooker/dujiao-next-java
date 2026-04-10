package com.dujiao.api.web.payment;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.web.ApiPaths;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/payments")
public class PaymentWebhookController {

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> callbackPost() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("received", true)));
    }

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> callbackGet() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("received", true)));
    }

    @PostMapping("/webhook/paypal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paypalWebhook() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("received", true)));
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stripeWebhook() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("received", true)));
    }
}
