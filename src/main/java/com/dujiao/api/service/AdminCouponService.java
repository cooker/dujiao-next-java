package com.dujiao.api.service;

import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.CouponEntity;
import com.dujiao.api.dto.coupon.CouponDto;
import com.dujiao.api.dto.coupon.CouponUpsertRequest;
import com.dujiao.api.repository.CouponRepository;
import com.dujiao.api.util.MoneyJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AdminCouponService {

    private static final String COUPON_TYPE_FIXED = "fixed";
    private static final String COUPON_TYPE_PERCENT = "percent";
    private static final String SCOPE_TYPE_PRODUCT = "product";

    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;

    public AdminCouponService(CouponRepository couponRepository, ObjectMapper objectMapper) {
        this.couponRepository = couponRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<List<CouponDto>> list(
            int page,
            int pageSize,
            String code,
            String idRaw,
            String scopeRefIdRaw,
            String isActiveRaw) {
        page = Math.max(page, 1);
        if (pageSize <= 0) {
            pageSize = 20;
        }
        if (pageSize > 200) {
            pageSize = 200;
        }

        Long idFilter = parseQueryIdStrict(idRaw);
        Long scopeRefFilter = parseQueryIdStrict(scopeRefIdRaw);
        Boolean isActive = parseOptionalBool(isActiveRaw);

        Specification<CouponEntity> spec = buildListSpec(idFilter, code, scopeRefFilter, isActive);
        Page<CouponEntity> result =
                couponRepository.findAll(
                        spec, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id")));
        List<CouponDto> list = result.getContent().stream().map(this::toDto).toList();
        PaginationDto pg =
                PaginationDto.of(page, pageSize, result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional(readOnly = true)
    public CouponDto get(long id) {
        return toDto(require(id));
    }

    @Transactional
    public CouponDto create(CouponUpsertRequest req) {
        validateCouponPayload(req);
        String code = req.code().trim();
        if (couponRepository.existsByCode(code)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_invalid");
        }
        CouponEntity e = new CouponEntity();
        apply(e, req, true);
        e.setUsedCount(0);
        return toDto(couponRepository.save(e));
    }

    @Transactional
    public CouponDto update(long id, CouponUpsertRequest req) {
        validateCouponPayload(req);
        CouponEntity e = require(id);
        String code = req.code().trim();
        if (!code.equals(e.getCode()) && couponRepository.existsByCodeAndIdNot(code, e.getId())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_invalid");
        }
        apply(e, req, false);
        return toDto(couponRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        CouponEntity e =
                couponRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "coupon_not_found"));
        couponRepository.delete(e);
    }

    private CouponEntity require(long id) {
        return couponRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "coupon_not_found"));
    }

    private void validateCouponPayload(CouponUpsertRequest req) {
        String couponType = req.type().trim().toLowerCase();
        if (!COUPON_TYPE_FIXED.equals(couponType) && !COUPON_TYPE_PERCENT.equals(couponType)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_invalid");
        }
        BigDecimal value = MoneyJson.normalize(req.value());
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_invalid");
        }
        if (COUPON_TYPE_PERCENT.equals(couponType)
                && value.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_invalid");
        }
        Instant starts = parseInstantNullable(req.startsAt());
        Instant ends = parseInstantNullable(req.endsAt());
        if (starts != null && ends != null && ends.isBefore(starts)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_invalid");
        }
    }

    private void apply(CouponEntity e, CouponUpsertRequest req, boolean isCreate) {
        String couponType = req.type().trim().toLowerCase();
        BigDecimal value = MoneyJson.normalize(req.value());
        BigDecimal minAmount = MoneyJson.normalize(req.minAmount());
        BigDecimal maxDiscount = MoneyJson.normalize(req.maxDiscount());
        int usageLimit = req.usageLimit() != null ? req.usageLimit() : 0;
        int perUserLimit = req.perUserLimit() != null ? req.perUserLimit() : 0;
        String scopeJson = encodeScopeRefIds(req.scopeRefIds());
        Instant starts = parseInstantNullable(req.startsAt());
        Instant ends = parseInstantNullable(req.endsAt());

        e.setCode(req.code().trim());
        e.setType(couponType);
        e.setValue(value);
        e.setMinAmount(minAmount);
        e.setMaxDiscount(maxDiscount);
        e.setUsageLimit(usageLimit);
        e.setPerUserLimit(perUserLimit);
        e.setScopeType(SCOPE_TYPE_PRODUCT);
        e.setScopeRefIds(scopeJson);
        e.setStartsAt(starts);
        e.setEndsAt(ends);
        if (req.isActive() != null) {
            e.setActive(req.isActive());
        } else if (isCreate) {
            e.setActive(true);
        }
    }

    private String encodeScopeRefIds(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_scope_invalid");
        }
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

    private static Long parseQueryIdStrict(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long v = Long.parseLong(raw.trim());
            if (v <= 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
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

    private static Specification<CouponEntity> buildListSpec(
            Long id, String code, Long scopeRefId, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (id != null && id > 0) {
                predicates.add(cb.equal(root.get("id"), id));
            }
            if (code != null && !code.isBlank()) {
                predicates.add(cb.equal(root.get("code"), code.trim()));
            }
            if (scopeRefId != null && scopeRefId > 0) {
                String exact = "[" + scopeRefId + "]";
                String prefix = "[" + scopeRefId + ",%";
                String middle = "%," + scopeRefId + ",%";
                String suffix = "%," + scopeRefId + "]";
                predicates.add(
                        cb.or(
                                cb.equal(root.get("scopeRefIds"), exact),
                                cb.like(root.get("scopeRefIds"), prefix),
                                cb.like(root.get("scopeRefIds"), middle),
                                cb.like(root.get("scopeRefIds"), suffix)));
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

    private CouponDto toDto(CouponEntity e) {
        return new CouponDto(
                e.getId(),
                e.getCode(),
                e.getType(),
                MoneyJson.format(e.getValue()),
                MoneyJson.format(e.getMinAmount()),
                MoneyJson.format(e.getMaxDiscount()),
                e.getUsageLimit(),
                e.getUsedCount(),
                e.getPerUserLimit(),
                e.getScopeType(),
                e.getScopeRefIds(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
