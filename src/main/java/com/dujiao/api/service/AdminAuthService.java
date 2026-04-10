package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.AdminAccount;
import com.dujiao.api.dto.auth.AdminChangePasswordRequest;
import com.dujiao.api.dto.auth.AdminLoginRequest;
import com.dujiao.api.dto.auth.AdminLoginResponse;
import com.dujiao.api.repository.AdminAccountRepository;
import com.dujiao.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthService(
            AdminAccountRepository adminAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.adminAccountRepository = adminAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest req) {
        AdminAccount a =
                adminAccountRepository
                        .findByUsername(req.username().trim())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.BAD_REQUEST, "invalid_credentials"));
        if (!passwordEncoder.matches(req.password(), a.getPasswordHash())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_credentials");
        }
        String token = jwtService.createAdminToken(a.getId());
        return new AdminLoginResponse(
                token,
                new AdminLoginResponse.AdminBrief(a.getId(), a.getUsername(), a.isSuperAdmin()));
    }

    @Transactional
    public void changePassword(long adminId, AdminChangePasswordRequest req) {
        AdminAccount a =
                adminAccountRepository
                        .findById(adminId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "admin_not_found"));
        if (!passwordEncoder.matches(req.oldPassword(), a.getPasswordHash())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_old_password");
        }
        a.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        adminAccountRepository.save(a);
    }
}
