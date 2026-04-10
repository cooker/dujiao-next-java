package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserApiCredentialEntity;
import com.dujiao.api.dto.admin.ApiCredentialAdminDto;
import com.dujiao.api.dto.admin.ApiCredentialApproveResponse;
import com.dujiao.api.dto.admin.ApiCredentialStatusRequest;
import com.dujiao.api.repository.UserApiCredentialRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminApiCredentialService {

    private final UserApiCredentialRepository userApiCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminApiCredentialService(
            UserApiCredentialRepository userApiCredentialRepository, PasswordEncoder passwordEncoder) {
        this.userApiCredentialRepository = userApiCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<ApiCredentialAdminDto> list() {
        return userApiCredentialRepository.findAllByOrderByIdDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ApiCredentialAdminDto get(long id) {
        return toDto(require(id));
    }

    @Transactional
    public ApiCredentialApproveResponse approve(long id) {
        UserApiCredentialEntity e = require(id);
        if (!"pending".equals(e.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "credential_not_pending");
        }
        String apiKey = "ak_" + UUID.randomUUID().toString().replace("-", "");
        String secretPlain = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        e.setApiKey(apiKey);
        e.setSecretHash(passwordEncoder.encode(secretPlain));
        e.setSecretSuffix(secretTail(secretPlain));
        e.setStatus("active");
        e.setActive(true);
        e.setApprovedAt(Instant.now());
        e.setRejectReason(null);
        userApiCredentialRepository.save(e);
        return new ApiCredentialApproveResponse(toDto(e), secretPlain);
    }

    @Transactional
    public ApiCredentialAdminDto reject(long id) {
        UserApiCredentialEntity e = require(id);
        if (!"pending".equals(e.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "credential_not_pending");
        }
        e.setStatus("rejected");
        e.setRejectReason("");
        e.setActive(false);
        return toDto(userApiCredentialRepository.save(e));
    }

    @Transactional
    public ApiCredentialAdminDto updateStatus(long id, ApiCredentialStatusRequest req) {
        UserApiCredentialEntity e = require(id);
        e.setStatus(req.status().trim());
        return toDto(userApiCredentialRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        if (!userApiCredentialRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "api_credential_not_found");
        }
        userApiCredentialRepository.deleteById(id);
    }

    private UserApiCredentialEntity require(long id) {
        return userApiCredentialRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "api_credential_not_found"));
    }

    private ApiCredentialAdminDto toDto(UserApiCredentialEntity e) {
        return new ApiCredentialAdminDto(e.getId(), e.getUserId(), e.getApiKey(), e.getStatus());
    }

    private static String secretTail(String plain) {
        if (plain == null || plain.length() < 4) {
            return "";
        }
        return plain.substring(plain.length() - 4);
    }
}
