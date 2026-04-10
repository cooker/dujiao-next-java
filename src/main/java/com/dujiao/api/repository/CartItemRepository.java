package com.dujiao.api.repository;

import com.dujiao.api.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByIdAsc(long userId);

    Optional<CartItem> findByUserIdAndProductId(long userId, long productId);

    void deleteByUserIdAndProductId(long userId, long productId);
}
