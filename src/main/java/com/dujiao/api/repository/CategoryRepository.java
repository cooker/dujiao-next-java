package com.dujiao.api.repository;

import com.dujiao.api.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAsc();

    Optional<Category> findBySlug(String slug);
}
