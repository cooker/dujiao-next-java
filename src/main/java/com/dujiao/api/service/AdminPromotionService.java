package com.dujiao.api.service;

import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.PromotionEntity;
import com.dujiao.api.dto.promotion.PromotionDto;
import com.dujiao.api.dto.promotion.PromotionUpsertRequest;
import com.dujiao.api.repository.PromotionRepository;
import com.dujiao.api.util.MoneyJson;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPromotionService {

    private static final String PROMOTION_TYPE_FIXED = "fixed";
    private static final String PROMOTION_TYPE_PERCENT = "percent";
    private static final String PROMOTION_TYPE_SPECIAL_PRICE = "special_price";
    private static final String SCOPE_TYPE_PRODUCT = "product";

    private final PromotionRepository promotionRepository;

    public AdminPromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<List<PromotionDto>> list(
            int page, int pageSize, String idRaw, String scopeRefIdRaw, String isActiveRaw) {
        page = Math.max(page, 1);
        if (pageSize <= 0) {
            pageSize = 20;
        }
        if (pageSize > 200) {
            pageSize = 200;
        }

        Long idFilter = parseOptionalQueryLong(idRaw, true);
        Long scopeRefFilter = parseOptionalQueryLong(scopeRefIdRaw, false);
        Boolean isActive = parseOptionalBool(isActiveRaw);

        Specification<PromotionEntity> spec = buildListSpec(idFilter, scopeRefFilter, isActive);
        Page<PromotionEntity> result =
                promotionRepository.findAll(
                        spec, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id")));
        List<PromotionDto> list = result.getContent().stream().map(this::toDto).toList();
        PaginationDto pg =
                PaginationDto.of(page, pageSize, result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional(readOnly = true)
    public PromotionDto get(long id) {
        return toDto(require(id));
    }

    @Transactional
    public PromotionDto create(PromotionUpsertRequest req) {
        validatePromotionPayload(req);
        PromotionEntity e = new PromotionEntity();
        apply(e, req, true);
        return toDto(promotionRepository.save(e));
    }

    @Transactional
    public PromotionDto update(long id, PromotionUpsertRequest req) {
        validatePromotionPayload(req);
        PromotionEntity e = require(id);
        apply(e, req, false);
        return toDto(promotionRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        PromotionEntity e =
                promotionRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "promotion_not_found"));
        promotionRepository.delete(e);
    }

    private PromotionEntity require(long id) {
        return promotionRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "promotion_not_found"));
    }

    private void validatePromotionPayload(PromotionUpsertRequest req) {
        if (req.scopeRefId() == null || req.scopeRefId() <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "promotion_invalid");
        }
        String pType = req.type().trim().toLowerCase();
        if (!PROMOTION_TYPE_FIXED.equals(pType)
                && !PROMOTION_TYPE_PERCENT.equals(pType)
                && !PROMOTION_TYPE_SPECIAL_PRICE.equals(pType)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "promotion_invalid");
        }
        BigDecimal value = MoneyJson.normalize(req.value());
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "promotion_invalid");
        }
        if (PROMOTION_TYPE_PERCENT.equals(pType)
                && value.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "promotion_invalid");
        }
        Instant starts = parseInstantNullable(req.startsAt());
        Instant ends = parseInstantNullable(req.endsAt());
        if (starts != null && ends != null && ends.isBefore(starts)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "promotion_invalid");
        }
    }

    private void apply(PromotionEntity e, PromotionUpsertRequest req, boolean isCreate) {
        String pType = req.type().trim().toLowerCase();
        BigDecimal value = MoneyJson.normalize(req.value());
        BigDecimal minAmount = MoneyJson.normalize(req.minAmount());
        Instant starts = parseInstantNullable(req.startsAt());
        Instant ends = parseInstantNullable(req.endsAt());

        e.setName(req.name().trim());
        e.setScopeType(SCOPE_TYPE_PRODUCT);
        e.setScopeRefId(req.scopeRefId());
        e.setType(pType);
        e.setValue(value);
        e.setMinAmount(minAmount);
        e.setStartsAt(starts);
        e.setEndsAt(ends);
        if (req.isActive() != null) {
            e.setActive(req.isActive());
        } else if (isCreate) {
            e.setActive(true);
        }
    }

    private static Boolean parseOptionalBool(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return Boolean.FALSE;
        }
        throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
    }

    private Instant parseInstantNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    /**
     * @param rejectZeroWhenPresent 与 Go {@code ParseQueryUint(..., zeroInvalid)} 一致：有输入且为 0 时报错。
     */
    private static Long parseOptionalQueryLong(String raw, boolean rejectZeroWhenPresent) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long v = Long.parseLong(raw.trim());
            if (rejectZeroWhenPresent && v == 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    private static Specification<PromotionEntity> buildListSpec(
            Long id, Long scopeRefId, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (id != null && id > 0) {
                predicates.add(cb.equal(root.get("id"), id));
            }
            if (scopeRefId != null && scopeRefId > 0) {
                predicates.add(cb.equal(root.get("scopeRefId"), scopeRefId));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("active"), isActive));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private PromotionDto toDto(PromotionEntity e) {
        return new PromotionDto(
                e.getId(),
                e.getName(),
                e.getScopeType(),
                e.getScopeRefId(),
                e.getType(),
                MoneyJson.format(e.getValue()),
                MoneyJson.format(e.getMinAmount()),
                e.getStartsAt(),
                e.getEndsAt(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
