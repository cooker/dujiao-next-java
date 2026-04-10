package com.dujiao.api.repository;

import com.dujiao.api.domain.ProductSku;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    List<ProductSku> findByProductIdAndDeletedAtIsNullOrderBySortOrderDescIdAsc(Long productId);

    Optional<ProductSku> findByProductIdAndSkuCodeAndDeletedAtIsNull(Long productId, String skuCode);

    Optional<ProductSku> findByIdAndProductIdAndDeletedAtIsNull(Long id, Long productId);
}
