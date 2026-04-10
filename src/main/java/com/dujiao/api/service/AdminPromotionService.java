package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.PromotionEntity;
import com.dujiao.api.dto.promotion.PromotionDto;
import com.dujiao.api.dto.promotion.PromotionUpsertRequest;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.repository.PromotionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;

    public AdminPromotionService(
            PromotionRepository promotionRepository, ProductRepository productRepository) {
        this.promotionRepository = promotionRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<PromotionDto> list() {
        return promotionRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PromotionDto get(long id) {
        return toDto(require(id));
    }

    @Transactional
    public PromotionDto create(PromotionUpsertRequest req) {
        ensureProduct(req.productId());
        PromotionEntity e = new PromotionEntity();
        apply(e, req, true);
        return toDto(promotionRepository.save(e));
    }

    @Transactional
    public PromotionDto update(long id, PromotionUpsertRequest req) {
        ensureProduct(req.productId());
        PromotionEntity e = require(id);
        apply(e, req, false);
        return toDto(promotionRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        if (!promotionRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "promotion_not_found");
        }
        promotionRepository.deleteById(id);
    }

    private void ensureProduct(Long productId) {
        if (productId != null && !productRepository.existsById(productId)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "product_not_found");
        }
    }

    private PromotionEntity require(long id) {
        return promotionRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "promotion_not_found"));
    }

    private void apply(PromotionEntity e, PromotionUpsertRequest req, boolean isCreate) {
        e.setName(req.name().trim());
        e.setProductId(req.productId());
        if (req.promotionPrice() != null) {
            e.setPromotionPrice(req.promotionPrice());
        }
        if (req.active() != null) {
            e.setActive(req.active());
        } else if (isCreate) {
            e.setActive(true);
        }
    }

    private PromotionDto toDto(PromotionEntity e) {
        return new PromotionDto(
                e.getId(), e.getName(), e.getProductId(), e.getPromotionPrice(), e.isActive());
    }
}
