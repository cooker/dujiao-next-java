package com.dujiao.api.repository;

import com.dujiao.api.domain.OrderLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    @Query(
            value =
                    "SELECT product_id, SUM(quantity) AS qty FROM order_lines GROUP BY product_id ORDER BY qty DESC"
                            + " LIMIT ?1",
            nativeQuery = true)
    List<Object[]> topProductsByQuantity(int limit);
}
