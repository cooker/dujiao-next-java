package com.dujiao.api.repository;

import com.dujiao.api.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByActiveTrueOrderBySortOrderAsc(Pageable pageable);

    List<Product> findByActiveTrueOrderBySortOrderAsc();

    boolean existsByCategoryId(long categoryId);

    long countByCategoryIdAndActiveTrue(long categoryId);
}
