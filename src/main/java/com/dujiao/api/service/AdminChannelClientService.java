package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.ChannelClientEntity;
import com.dujiao.api.dto.channel.ChannelClientCreateRequest;
import com.dujiao.api.dto.channel.ChannelClientCreateResponse;
import com.dujiao.api.dto.channel.ChannelClientDto;
import com.dujiao.api.dto.channel.ChannelClientSecretResetResponse;
import com.dujiao.api.dto.channel.ChannelClientStatusRequest;
import com.dujiao.api.crypto.ChannelSecretCrypto;
import com.dujiao.api.dto.channel.ChannelClientUpdateRequest;
import com.dujiao.api.repository.ChannelClientRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminChannelClientService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ChannelClientRepository channelClientRepository;
    private final byte[] channelCryptoKey;

    public AdminChannelClientService(
            ChannelClientRepository channelClientRepository,
            @Value("${dujiao.crypto.master-key}") String masterKey) {
        this.channelClientRepository = channelClientRepository;
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException("dujiao.crypto.master-key is required");
        }
        this.channelCryptoKey = ChannelSecretCrypto.deriveKey(masterKey);
    }

    @Transactional(readOnly = true)
    public List<ChannelClientDto> list() {
        return channelClientRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChannelClientDto get(long id) {
        return toDto(
                channelClientRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "channel_client_not_found")));
    }

    @Transactional
    public ChannelClientCreateResponse create(ChannelClientCreateRequest req) {
        String clientId;
        if (req.clientId() != null && !req.clientId().isBlank()) {
            clientId = req.clientId().trim();
            if (clientId.length() < 8
                    || clientId.length() > 64
                    || !clientId.matches("^[a-zA-Z0-9_-]+$")) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_client_id");
            }
        } else {
            clientId = newClientId();
        }
        if (channelClientRepository.existsByClientId(clientId)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "client_id_exists");
        }
        String plainSecret;
        if (req.clientSecret() != null && !req.clientSecret().isBlank()) {
            plainSecret = req.clientSecret();
            if (plainSecret.length() < 16 || plainSecret.length() > 256) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_client_secret");
            }
        } else {
            plainSecret = newClientSecret();
        }
        ChannelClientEntity e = new ChannelClientEntity();
        e.setName(req.name().trim());
        e.setClientId(clientId);
        e.setSecretCipher(ChannelSecretCrypto.encrypt(channelCryptoKey, plainSecret));
        e.setStatus("active");
        e = channelClientRepository.save(e);
        return new ChannelClientCreateResponse(toDto(e), plainSecret);
    }

    @Transactional
    public ChannelClientDto update(long id, ChannelClientUpdateRequest req) {
        ChannelClientEntity e =
                channelClientRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "channel_client_not_found"));
        e.setName(req.name().trim());
        return toDto(channelClientRepository.save(e));
    }

    @Transactional
    public ChannelClientDto updateStatus(long id, ChannelClientStatusRequest req) {
        ChannelClientEntity e =
                channelClientRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "channel_client_not_found"));
        e.setStatus(req.status());
        return toDto(channelClientRepository.save(e));
    }

    @Transactional
    public ChannelClientSecretResetResponse resetSecret(long id) {
        ChannelClientEntity e =
                channelClientRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "channel_client_not_found"));
        String plain = newClientSecret();
        e.setSecretCipher(ChannelSecretCrypto.encrypt(channelCryptoKey, plain));
        channelClientRepository.save(e);
        return new ChannelClientSecretResetResponse(plain);
    }

    @Transactional
    public void delete(long id) {
        if (!channelClientRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "channel_client_not_found");
        }
        channelClientRepository.deleteById(id);
    }

    private ChannelClientDto toDto(ChannelClientEntity e) {
        return new ChannelClientDto(e.getId(), e.getName(), e.getClientId(), e.getStatus());
    }

    private static String newClientId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String newClientSecret() {
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
