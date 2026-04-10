package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.GiftCardBatchEntity;
import com.dujiao.api.domain.GiftCardEntity;
import com.dujiao.api.dto.giftcard.GiftCardBatchDto;
import com.dujiao.api.dto.giftcard.GiftCardBatchStatusRequest;
import com.dujiao.api.dto.giftcard.GiftCardDto;
import com.dujiao.api.dto.giftcard.GiftCardGenerateRequest;
import com.dujiao.api.dto.giftcard.GiftCardGenerateResponse;
import com.dujiao.api.dto.giftcard.GiftCardUpdateRequest;
import com.dujiao.api.repository.GiftCardBatchRepository;
import com.dujiao.api.repository.GiftCardRepository;
import com.dujiao.api.util.GiftCardCodegen;
import com.dujiao.api.util.MoneyJson;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminGiftCardService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_REDEEMED = "redeemed";
    private static final String STATUS_DISABLED = "disabled";

    private final GiftCardRepository giftCardRepository;
    private final GiftCardBatchRepository giftCardBatchRepository;
    private final SettingsService settingsService;

    public AdminGiftCardService(
            GiftCardRepository giftCardRepository,
            GiftCardBatchRepository giftCardBatchRepository,
            SettingsService settingsService) {
        this.giftCardRepository = giftCardRepository;
        this.giftCardBatchRepository = giftCardBatchRepository;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public List<GiftCardDto> list() {
        return giftCardRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GiftCardDto get(long id) {
        return toDto(require(id));
    }

    /**
     * 与 Go {@code GiftCardService.GenerateGiftCards} 一致：先插入批次，再批量插入卡密；响应为 {@code batch} +
     * {@code created}。
     */
    @Transactional
    public GiftCardGenerateResponse generate(GiftCardGenerateRequest req, long createdByAdminId) {
        String name = req.name().trim();
        if (name.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
        }
        int quantity = req.quantity();
        if (quantity <= 0 || quantity > 10000) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(req.amount().trim()).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
        }
        Instant expiresNorm = normalizeExpiresAt(req.expiresAt());
        String currency = settingsService.getSiteCurrency();
        Instant now = Instant.now();

        GiftCardBatchEntity batch = new GiftCardBatchEntity();
        batch.setBatchNo(GiftCardCodegen.generateBatchNo(now));
        batch.setName(name);
        batch.setAmount(amount);
        batch.setCurrency(currency);
        batch.setQuantity(quantity);
        batch.setExpiresAt(expiresNorm);
        batch.setCreatedBy(createdByAdminId);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        batch = giftCardBatchRepository.saveAndFlush(batch);

        List<GiftCardEntity> toSave = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            GiftCardEntity c = new GiftCardEntity();
            c.setBatchId(batch.getId());
            c.setName(name);
            c.setCode(GiftCardCodegen.generateCardCode(now, i));
            c.setAmount(amount);
            c.setCurrency(currency);
            c.setStatus(STATUS_ACTIVE);
            c.setExpiresAt(expiresNorm);
            c.setCreatedAt(now);
            c.setUpdatedAt(now);
            toSave.add(c);
        }
        giftCardRepository.saveAll(toSave);

        return new GiftCardGenerateResponse(toBatchDto(batch), quantity);
    }

    @Transactional
    public GiftCardDto update(long id, GiftCardUpdateRequest req) {
        GiftCardEntity e = require(id);
        if (req.name() != null) {
            String n = req.name().trim();
            if (n.isEmpty()) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
            }
            e.setName(n);
        }
        if (req.status() != null) {
            String st = req.status().trim().toLowerCase(Locale.ROOT);
            switch (st) {
                case STATUS_ACTIVE, STATUS_DISABLED -> {
                    if (STATUS_REDEEMED.equals(e.getStatus())) {
                        throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
                    }
                    e.setStatus(st);
                }
                default -> throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
            }
        }
        if (req.expiresAt() != null) {
            if (req.expiresAt().isBlank()) {
                e.setExpiresAt(null);
            } else {
                Instant parsed = parseExpiresAtRequired(req.expiresAt().trim());
                if (parsed.isBefore(Instant.now())) {
                    throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
                }
                e.setExpiresAt(parsed);
            }
        }
        e.setUpdatedAt(Instant.now());
        return toDto(giftCardRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        GiftCardEntity e = require(id);
        if (STATUS_REDEEMED.equals(e.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
        }
        giftCardRepository.delete(e);
    }

    @Transactional
    public int batchStatus(GiftCardBatchStatusRequest req) {
        String st = req.status().trim().toLowerCase(Locale.ROOT);
        if (!STATUS_ACTIVE.equals(st) && !STATUS_DISABLED.equals(st)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
        }
        int n = 0;
        for (Long id : req.ids()) {
            var opt = giftCardRepository.findById(id);
            if (opt.isPresent()) {
                GiftCardEntity e = opt.get();
                e.setStatus(st);
                e.setUpdatedAt(Instant.now());
                giftCardRepository.save(e);
                n++;
            }
        }
        return n;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("id,batch_id,name,code,amount,currency,status\n");
        for (GiftCardEntity e : giftCardRepository.findAll()) {
            sb.append(e.getId())
                    .append(',')
                    .append(e.getBatchId() != null ? e.getBatchId() : "")
                    .append(',')
                    .append(escapeCsv(e.getName()))
                    .append(',')
                    .append(escapeCsv(e.getCode()))
                    .append(',')
                    .append(e.getAmount() != null ? e.getAmount().toPlainString() : "")
                    .append(',')
                    .append(escapeCsv(e.getCurrency()))
                    .append(',')
                    .append(escapeCsv(e.getStatus()))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private GiftCardEntity require(long id) {
        return giftCardRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "gift_card_not_found"));
    }

    private GiftCardDto toDto(GiftCardEntity e) {
        return new GiftCardDto(
                e.getId(),
                e.getBatchId(),
                e.getName(),
                e.getCode(),
                MoneyJson.format(e.getAmount()),
                e.getCurrency(),
                e.getStatus(),
                e.getExpiresAt(),
                e.getRedeemedAt(),
                e.getRedeemedUserId(),
                e.getWalletTxnId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    private static GiftCardBatchDto toBatchDto(GiftCardBatchEntity b) {
        return new GiftCardBatchDto(
                b.getId(),
                b.getBatchNo(),
                b.getName(),
                MoneyJson.format(b.getAmount()),
                b.getCurrency(),
                b.getQuantity(),
                b.getExpiresAt(),
                b.getCreatedBy(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }

    private static Instant normalizeExpiresAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    private static Instant parseExpiresAtRequired(String raw) {
        try {
            return Instant.parse(raw);
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
