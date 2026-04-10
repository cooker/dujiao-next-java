package com.dujiao.api.service.admin;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.PaymentChannelEntity;
import com.dujiao.api.dto.payment.PaymentChannelDto;
import com.dujiao.api.dto.payment.PaymentChannelUpsertRequest;
import com.dujiao.api.repository.PaymentChannelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPaymentChannelService {

    private final PaymentChannelRepository paymentChannelRepository;
    private final ObjectMapper objectMapper;

    public AdminPaymentChannelService(PaymentChannelRepository paymentChannelRepository, ObjectMapper objectMapper) {
        this.paymentChannelRepository = paymentChannelRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentChannelDto> list() {
        return paymentChannelRepository.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PaymentChannelDto get(long id) {
        return toDto(require(id));
    }

    @Transactional
    public PaymentChannelDto create(PaymentChannelUpsertRequest req) {
        PaymentChannelEntity e = new PaymentChannelEntity();
        apply(e, req, true);
        return toDto(paymentChannelRepository.save(e));
    }

    @Transactional
    public PaymentChannelDto update(long id, PaymentChannelUpsertRequest req) {
        PaymentChannelEntity e = require(id);
        apply(e, req, false);
        return toDto(paymentChannelRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        if (!paymentChannelRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "payment_channel_not_found");
        }
        paymentChannelRepository.deleteById(id);
    }

    private PaymentChannelEntity require(long id) {
        return paymentChannelRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "payment_channel_not_found"));
    }

    private void apply(PaymentChannelEntity e, PaymentChannelUpsertRequest req, boolean isCreate) {
        e.setName(req.name().trim());
        e.setChannelType(req.channelType().trim());
        e.setConfigJson(configToString(req.configJson()));
        if (req.active() != null) {
            e.setActive(req.active());
        } else if (isCreate) {
            e.setActive(true);
        }
    }

    private String configToString(JsonNode node) {
        if (node == null || node.isNull()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_config_json");
        }
    }

    private PaymentChannelDto toDto(PaymentChannelEntity e) {
        JsonNode config;
        try {
            String raw = e.getConfigJson();
            config = raw == null || raw.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(raw);
        } catch (JsonProcessingException ex) {
            config = objectMapper.createObjectNode();
        }
        return new PaymentChannelDto(e.getId(), e.getName(), e.getChannelType(), config, e.isActive());
    }
}
