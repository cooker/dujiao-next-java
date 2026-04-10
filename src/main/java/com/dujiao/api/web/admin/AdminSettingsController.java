package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AuthMailHelper;
import com.dujiao.api.service.SettingsService;
import com.dujiao.api.web.ApiPaths;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/settings")
public class AdminSettingsController {

    private static final String SK_SMTP = "smtp";
    private static final String SK_CAPTCHA = "captcha";
    private static final String SK_TELEGRAM_AUTH = "telegram_auth";
    private static final String SK_NOTIFICATION_CENTER = "notification_center";
    private static final String SK_ORDER_EMAIL_TEMPLATE = "order_email_template";
    private static final String SK_AFFILIATE = "affiliate";
    private static final String SK_TELEGRAM_BOT = "telegram_bot";

    private final SettingsService settingsService;
    private final AuthMailHelper authMailHelper;

    public AdminSettingsController(SettingsService settingsService, AuthMailHelper authMailHelper) {
        this.settingsService = settingsService;
        this.authMailHelper = authMailHelper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettingsMap()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(
            @RequestBody Map<String, Object> body) {
        SecurityUtils.requireAdminId();
        settingsService.mergeSettingsMap(body);
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettingsMap()));
    }

    @GetMapping("/smtp")
    public ResponseEntity<ApiResponse<JsonNode>> getSmtp() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_SMTP)));
    }

    @PutMapping("/smtp")
    public ResponseEntity<ApiResponse<JsonNode>> updateSmtp(@RequestBody JsonNode body) {
        SecurityUtils.requireAdminId();
        settingsService.mergeJson(SK_SMTP, body);
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_SMTP)));
    }

    @PostMapping("/smtp/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testSmtp(
            @RequestBody(required = false) SmtpTestRequest req) {
        SecurityUtils.requireAdminId();
        if (req == null || req.toEmail() == null || req.toEmail().isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_invalid");
        }
        boolean sent = authMailHelper.sendCustomEmail(req.toEmail().trim(), req.subject(), req.body());
        if (sent) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true, "sent", true)));
        }
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of(
                                "ok", true,
                                "sent", false,
                                "message", "email_service_not_configured")));
    }

    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<JsonNode>> getCaptcha() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_CAPTCHA)));
    }

    @PutMapping("/captcha")
    public ResponseEntity<ApiResponse<JsonNode>> updateCaptcha(@RequestBody JsonNode body) {
        SecurityUtils.requireAdminId();
        settingsService.mergeJson(SK_CAPTCHA, body);
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_CAPTCHA)));
    }

    @GetMapping("/telegram-auth")
    public ResponseEntity<ApiResponse<JsonNode>> getTelegramAuth() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_TELEGRAM_AUTH)));
    }

    @PutMapping("/telegram-auth")
    public ResponseEntity<ApiResponse<JsonNode>> updateTelegramAuth(@RequestBody JsonNode body) {
        SecurityUtils.requireAdminId();
        settingsService.mergeJson(SK_TELEGRAM_AUTH, body);
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_TELEGRAM_AUTH)));
    }

    @GetMapping("/notification-center")
    public ResponseEntity<ApiResponse<JsonNode>> getNotificationCenter() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_NOTIFICATION_CENTER)));
    }

    @PutMapping("/notification-center")
    public ResponseEntity<ApiResponse<JsonNode>> updateNotificationCenter(@RequestBody JsonNode body) {
        SecurityUtils.requireAdminId();
        settingsService.mergeJson(SK_NOTIFICATION_CENTER, body);
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_NOTIFICATION_CENTER)));
    }

    @GetMapping("/notification-center/logs")
    public ResponseEntity<ApiResponse<List<Object>>> notificationCenterLogs() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/notification-center/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testNotificationCenter(
            @RequestBody(required = false) NotificationTestRequest req) {
        SecurityUtils.requireAdminId();
        if (req == null || req.channel() == null || req.channel().isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if (req.target() == null || req.target().isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        String channel = req.channel().trim().toLowerCase(Locale.ROOT);
        if (!"email".equals(channel) && !"telegram".equals(channel)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if ("email".equals(channel)) {
            boolean sent =
                    authMailHelper.sendCustomEmail(
                            req.target().trim(),
                            req.scene() == null || req.scene().isBlank()
                                    ? "通知中心测试邮件"
                                    : "[test] " + req.scene().trim(),
                            "notification_center_test");
            return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true, "sent", sent)));
        }
        // Java 端暂未接 Telegram 通知发送器；保持接口可调用并给出明确结果。
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("ok", true, "sent", false, "message", "telegram_not_configured")));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<JsonNode>> getNotificationsAlias() {
        return getNotificationCenter();
    }

    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<JsonNode>> updateNotificationsAlias(@RequestBody JsonNode body) {
        return updateNotificationCenter(body);
    }

    @GetMapping("/notifications/logs")
    public ResponseEntity<ApiResponse<List<Object>>> notificationsLogs() {
        return notificationCenterLogs();
    }

    @PostMapping("/notifications/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testNotifications(
            @RequestBody(required = false) NotificationTestRequest req) {
        return testNotificationCenter(req);
    }

    @GetMapping("/order-email-template")
    public ResponseEntity<ApiResponse<JsonNode>> getOrderEmailTemplate() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_ORDER_EMAIL_TEMPLATE)));
    }

    @PutMapping("/order-email-template")
    public ResponseEntity<ApiResponse<JsonNode>> updateOrderEmailTemplate(@RequestBody JsonNode body) {
        SecurityUtils.requireAdminId();
        settingsService.mergeJson(SK_ORDER_EMAIL_TEMPLATE, body);
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_ORDER_EMAIL_TEMPLATE)));
    }

    @PostMapping("/order-email-template/reset")
    public ResponseEntity<ApiResponse<JsonNode>> resetOrderEmailTemplate() {
        SecurityUtils.requireAdminId();
        settingsService.resetOrderEmailTemplate();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_ORDER_EMAIL_TEMPLATE)));
    }

    @GetMapping("/affiliate")
    public ResponseEntity<ApiResponse<JsonNode>> getAffiliateSettings() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_AFFILIATE)));
    }

    @PutMapping("/affiliate")
    public ResponseEntity<ApiResponse<JsonNode>> updateAffiliateSettings(@RequestBody JsonNode body) {
        SecurityUtils.requireAdminId();
        settingsService.mergeJson(SK_AFFILIATE, body);
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_AFFILIATE)));
    }

    @GetMapping("/telegram-bot")
    public ResponseEntity<ApiResponse<JsonNode>> getTelegramBot() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_TELEGRAM_BOT)));
    }

    @PutMapping("/telegram-bot")
    public ResponseEntity<ApiResponse<JsonNode>> updateTelegramBot(@RequestBody JsonNode body) {
        SecurityUtils.requireAdminId();
        settingsService.mergeJson(SK_TELEGRAM_BOT, body);
        return ResponseEntity.ok(ApiResponse.success(settingsService.getJson(SK_TELEGRAM_BOT)));
    }

    @GetMapping("/telegram-bot/runtime-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> telegramBotRuntimeStatus() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("connected", false, "note", "runtime_status_stub")));
    }

    private record SmtpTestRequest(
            @JsonProperty("to_email") String toEmail, String subject, String body) {}

    private record NotificationTestRequest(
            String channel,
            String target,
            String scene,
            String locale,
            Map<String, Object> variables) {}
}
