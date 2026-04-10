package com.dujiao.api.web.auth;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.dto.auth.ForgotPasswordRequest;
import com.dujiao.api.dto.auth.LoginRequest;
import com.dujiao.api.dto.auth.LoginResponse;
import com.dujiao.api.dto.auth.RegisterRequest;
import com.dujiao.api.dto.auth.SendVerifyCodeRequest;
import com.dujiao.api.dto.auth.TelegramLoginRequest;
import com.dujiao.api.dto.auth.TelegramMiniAppLoginRequest;
import com.dujiao.api.service.AuthService;
import com.dujiao.api.service.TelegramAuthService;
import com.dujiao.api.service.UserLoginLogService;
import com.dujiao.api.web.HttpClientSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(com.dujiao.api.web.ApiPaths.V1 + "/auth")
public class AuthApiController {

    private final AuthService authService;
    private final TelegramAuthService telegramAuthService;
    private final UserLoginLogService userLoginLogService;

    public AuthApiController(
            AuthService authService,
            TelegramAuthService telegramAuthService,
            UserLoginLogService userLoginLogService) {
        this.authService = authService;
        this.telegramAuthService = telegramAuthService;
        this.userLoginLogService = userLoginLogService;
    }

    @PostMapping("/send-verify-code")
    public ResponseEntity<ApiResponse<Void>> sendVerifyCode(
            @Valid @RequestBody SendVerifyCodeRequest req) {
        authService.sendVerifyCode(req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(req)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String ip = HttpClientSnapshot.clientIp(http);
        String ua = HttpClientSnapshot.userAgent(http);
        try {
            LoginResponse r = authService.login(req);
            userLoginLogService.recordSuccess(r.user().id(), req.email(), ip, ua, "web");
            return ResponseEntity.ok(ApiResponse.success(r));
        } catch (BusinessException e) {
            userLoginLogService.recordFailure(0, req.email(), UserLoginLogService.mapWebLoginFailReason(e), ip, ua, "web");
            throw e;
        }
    }

    @PostMapping("/telegram/login")
    public ResponseEntity<ApiResponse<LoginResponse>> telegramLogin(
            @Valid @RequestBody TelegramLoginRequest req, HttpServletRequest http) {
        String ip = HttpClientSnapshot.clientIp(http);
        String ua = HttpClientSnapshot.userAgent(http);
        try {
            LoginResponse r = telegramAuthService.loginWithTelegramWidget(req);
            userLoginLogService.recordSuccess(r.user().id(), r.user().email(), ip, ua, "telegram");
            return ResponseEntity.ok(ApiResponse.success(r));
        } catch (BusinessException e) {
            userLoginLogService.recordFailure(0, "", e.getMessage() != null ? e.getMessage() : "internal_error", ip, ua, "telegram");
            throw e;
        }
    }

    @PostMapping("/telegram/miniapp/login")
    public ResponseEntity<ApiResponse<LoginResponse>> telegramMiniAppLogin(
            @Valid @RequestBody TelegramMiniAppLoginRequest req, HttpServletRequest http) {
        String ip = HttpClientSnapshot.clientIp(http);
        String ua = HttpClientSnapshot.userAgent(http);
        try {
            LoginResponse r = telegramAuthService.loginWithMiniApp(req);
            userLoginLogService.recordSuccess(r.user().id(), r.user().email(), ip, ua, "telegram");
            return ResponseEntity.ok(ApiResponse.success(r));
        } catch (BusinessException e) {
            userLoginLogService.recordFailure(0, "", e.getMessage() != null ? e.getMessage() : "internal_error", ip, ua, "telegram");
            throw e;
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
