package com.dujiao.api.repository;

import com.dujiao.api.domain.OrderStatus;
import com.dujiao.api.domain.ShopOrder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    Optional<ShopOrder> findByOrderNo(String orderNo);

    Optional<ShopOrder> findByOrderNoAndUserId(String orderNo, long userId);

    Page<ShopOrder> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);

    Page<ShopOrder> findByUserIdAndStatusOrderByCreatedAtDesc(
            long userId, OrderStatus status, Pageable pageable);

    Page<ShopOrder> findByUserIdAndOrderNoContainingIgnoreCaseOrderByCreatedAtDesc(
            long userId, String orderNo, Pageable pageable);

    Page<ShopOrder> findByUserIdAndStatusAndOrderNoContainingIgnoreCaseOrderByCreatedAtDesc(
            long userId, OrderStatus status, String orderNo, Pageable pageable);

    Page<ShopOrder> findByGuestEmailOrderByCreatedAtDesc(String guestEmail, Pageable pageable);

    List<ShopOrder> findByGuestEmailOrderByCreatedAtDesc(String guestEmail);

    long countByUserId(long userId);

    @Query(
            value =
                    "SELECT CAST(created_at AS DATE) AS d, COUNT(*) AS c FROM shop_orders WHERE created_at >= ?1 "
                            + "GROUP BY CAST(created_at AS DATE) ORDER BY d",
            nativeQuery = true)
    List<Object[]> countOrdersByDaySince(Instant since);
}
