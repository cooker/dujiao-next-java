package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.CouponEntity;
import com.dujiao.api.dto.coupon.CouponDto;
import com.dujiao.api.dto.coupon.CouponUpsertRequest;
import com.dujiao.api.repository.CouponRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCouponService {

    private final CouponRepository couponRepository;

    public AdminCouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional(readOnly = true)
    public List<CouponDto> list() {
        return couponRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CouponDto get(long id) {
        return toDto(require(id));
    }

    @Transactional
    public CouponDto create(CouponUpsertRequest req) {
        String code = req.code().trim();
        if (couponRepository.existsByCode(code)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_code_exists");
        }
        CouponEntity e = new CouponEntity();
        apply(e, req, true);
        return toDto(couponRepository.save(e));
    }

    @Transactional
    public CouponDto update(long id, CouponUpsertRequest req) {
        CouponEntity e = require(id);
        String code = req.code().trim();
        if (!code.equals(e.getCode()) && couponRepository.existsByCode(code)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "coupon_code_exists");
        }
        apply(e, req, false);
        return toDto(couponRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        if (!couponRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "coupon_not_found");
        }
        couponRepository.deleteById(id);
    }

    private CouponEntity require(long id) {
        return couponRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "coupon_not_found"));
    }

    private void apply(CouponEntity e, CouponUpsertRequest req, boolean isCreate) {
        e.setCode(req.code().trim());
        e.setName(req.name().trim());
        if (req.discountPercent() != null) {
            e.setDiscountPercent(req.discountPercent());
        }
        if (req.active() != null) {
            e.setActive(req.active());
        } else if (isCreate) {
            e.setActive(true);
        }
    }

    private CouponDto toDto(CouponEntity e) {
        return new CouponDto(
                e.getId(), e.getCode(), e.getName(), e.getDiscountPercent(), e.isActive());
    }
}
