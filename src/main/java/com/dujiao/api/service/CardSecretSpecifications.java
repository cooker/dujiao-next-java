package com.dujiao.api.service;

import com.dujiao.api.domain.CardSecretBatchEntity;
import com.dujiao.api.domain.CardSecretEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CardSecretSpecifications {

    private CardSecretSpecifications() {}

    public record ListFilter(
            long productId,
            long skuId,
            long batchId,
            String status,
            String secret,
            String batchNo) {}

    public static Specification<CardSecretEntity> matches(ListFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.productId() > 0) {
                p.add(cb.equal(root.get("productId"), f.productId()));
            }
            if (f.skuId() > 0) {
                p.add(cb.equal(root.get("skuId"), f.skuId()));
            }
            if (f.batchId() > 0) {
                Join<CardSecretEntity, CardSecretBatchEntity> jb =
                        root.join("batch", JoinType.INNER);
                p.add(cb.equal(jb.get("id"), f.batchId()));
            }
            if (f.status() != null && !f.status().isBlank()) {
                p.add(cb.equal(root.get("status"), f.status().trim()));
            }
            if (f.secret() != null && !f.secret().isBlank()) {
                p.add(
                        cb.like(
                                cb.lower(root.get("secret")),
                                "%" + f.secret().trim().toLowerCase() + "%"));
            }
            if (f.batchNo() != null && !f.batchNo().isBlank()) {
                Join<CardSecretEntity, CardSecretBatchEntity> b =
                        root.join("batch", JoinType.LEFT);
                p.add(
                        cb.like(
                                cb.lower(b.get("batchNo")),
                                "%" + f.batchNo().trim().toLowerCase() + "%"));
            }
            if (p.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
