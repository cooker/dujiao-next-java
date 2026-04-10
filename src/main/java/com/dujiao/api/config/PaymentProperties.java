package com.dujiao.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 在线支付占位与联调：生产环境请接入真实网关并关闭 {@code simulateCompletionEnabled}。
 */
@ConfigurationProperties(prefix = "dujiao.payment")
public class PaymentProperties {

    /**
     * 返回给前端的收银台/落地页基地址（可为空表示使用相对路径 {@code /pay?payment_id=}）。
     * 示例：https://shop.example.com/checkout
     */
    private String checkoutBaseUrl = "";

    /**
     * 是否开放 {@code POST /api/v1/payments/{id}/simulate-completion}（仅用于开发/联调）。
     */
    private boolean simulateCompletionEnabled = false;

    public String getCheckoutBaseUrl() {
        return checkoutBaseUrl;
    }

    public void setCheckoutBaseUrl(String checkoutBaseUrl) {
        this.checkoutBaseUrl = checkoutBaseUrl == null ? "" : checkoutBaseUrl.trim();
    }

    public boolean isSimulateCompletionEnabled() {
        return simulateCompletionEnabled;
    }

    public void setSimulateCompletionEnabled(boolean simulateCompletionEnabled) {
        this.simulateCompletionEnabled = simulateCompletionEnabled;
    }
}
