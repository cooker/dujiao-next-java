package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.GiftCardEntity;
import com.dujiao.api.dto.giftcard.GiftCardBatchStatusRequest;
import com.dujiao.api.dto.giftcard.GiftCardDto;
import com.dujiao.api.dto.giftcard.GiftCardGenerateRequest;
import com.dujiao.api.dto.giftcard.GiftCardUpdateRequest;
import com.dujiao.api.repository.GiftCardRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminGiftCardService {

    private final GiftCardRepository giftCardRepository;

    public AdminGiftCardService(GiftCardRepository giftCardRepository) {
        this.giftCardRepository = giftCardRepository;
    }

    @Transactional(readOnly = true)
    public List<GiftCardDto> list() {
        return giftCardRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GiftCardDto get(long id) {
        return toDto(require(id));
    }

    @Transactional
    public List<GiftCardDto> generate(GiftCardGenerateRequest req) {
        List<GiftCardDto> created = new ArrayList<>();
        for (int i = 0; i < req.count(); i++) {
            GiftCardEntity e = new GiftCardEntity();
            e.setCode(uniqueCode());
            e.setBalance(req.balance());
            e.setStatus("active");
            created.add(toDto(giftCardRepository.save(e)));
        }
        return created;
    }

    @Transactional
    public GiftCardDto update(long id, GiftCardUpdateRequest req) {
        GiftCardEntity e = require(id);
        if (req.balance() != null) {
            e.setBalance(req.balance());
        }
        if (req.status() != null && !req.status().isBlank()) {
            e.setStatus(req.status().trim());
        }
        return toDto(giftCardRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        if (!giftCardRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "gift_card_not_found");
        }
        giftCardRepository.deleteById(id);
    }

    @Transactional
    public int batchStatus(GiftCardBatchStatusRequest req) {
        String st = req.status().trim();
        int n = 0;
        for (Long id : req.giftCardIds()) {
            var opt = giftCardRepository.findById(id);
            if (opt.isPresent()) {
                GiftCardEntity e = opt.get();
                e.setStatus(st);
                giftCardRepository.save(e);
                n++;
            }
        }
        return n;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("id,code,balance,status\n");
        for (GiftCardEntity e : giftCardRepository.findAll()) {
            sb.append(e.getId())
                    .append(',')
                    .append(escapeCsv(e.getCode()))
                    .append(',')
                    .append(e.getBalance() != null ? e.getBalance().toPlainString() : "")
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
        return new GiftCardDto(e.getId(), e.getCode(), e.getBalance(), e.getStatus());
    }

    private String uniqueCode() {
        for (int i = 0; i < 20; i++) {
            String c = "GC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            if (giftCardRepository.findByCode(c).isEmpty()) {
                return c;
            }
        }
        throw new BusinessException(ResponseCodes.INTERNAL, "gift_card_code_gen_failed");
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
