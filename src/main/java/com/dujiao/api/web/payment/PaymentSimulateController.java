package com.dujiao.api.web.payment;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.OrderOnlinePaymentService;
import com.dujiao.api.web.ApiPaths;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发/联调：将在线支付单置为成功（生产务必关闭 {@code dujiao.payment.simulate-completion-enabled}）。
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/payments")
@ConditionalOnProperty(prefix = "dujiao.payment", name = "simulate-completion-enabled", havingValue = "true")
public class PaymentSimulateController {

    private final OrderOnlinePaymentService orderOnlinePaymentService;

    public PaymentSimulateController(OrderOnlinePaymentService orderOnlinePaymentService) {
        this.orderOnlinePaymentService = orderOnlinePaymentService;
    }

    @PostMapping("/{id}/simulate-completion")
    public ResponseEntity<ApiResponse<Void>> simulateCompletion(@PathVariable long id) {
        long uid = SecurityUtils.requireUserId();
        orderOnlinePaymentService.simulateCompletion(id, uid);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
