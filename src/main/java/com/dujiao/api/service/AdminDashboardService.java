package com.dujiao.api.service;

import com.dujiao.api.repository.OrderLineRepository;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.util.LocalizedTitleJson;
import com.dujiao.api.repository.ShopOrderRepository;
import com.dujiao.api.repository.UserAccountRepository;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private static final int TREND_DAYS = 7;
    private static final int RANKING_LIMIT = 10;

    private final UserAccountRepository userAccountRepository;
    private final ProductRepository productRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final OrderLineRepository orderLineRepository;

    public AdminDashboardService(
            UserAccountRepository userAccountRepository,
            ProductRepository productRepository,
            ShopOrderRepository shopOrderRepository,
            OrderLineRepository orderLineRepository) {
        this.userAccountRepository = userAccountRepository;
        this.productRepository = productRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.orderLineRepository = orderLineRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        return Map.of(
                "users_total", userAccountRepository.count(),
                "products_total", productRepository.count(),
                "orders_total", shopOrderRepository.count());
    }

    /** 近 N 天每日订单数（按日聚合）。 */
    @Transactional(readOnly = true)
    public Map<String, Object> trends() {
        Instant since = Instant.now().minus(TREND_DAYS, ChronoUnit.DAYS);
        List<Object[]> rows = shopOrderRepository.countOrdersByDaySince(since);
        List<Map<String, Object>> series = new ArrayList<>();
        for (Object[] row : rows) {
            String day = row[0] instanceof Date ? ((Date) row[0]).toString() : String.valueOf(row[0]);
            long cnt = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            series.add(Map.of("date", day, "orders", cnt));
        }
        return Map.of("days", TREND_DAYS, "series", series);
    }

    /** 按订单行销量汇总的前若干商品。 */
    @Transactional(readOnly = true)
    public Map<String, Object> rankings() {
        List<Object[]> rows = orderLineRepository.topProductsByQuantity(RANKING_LIMIT);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            long pid = row[0] instanceof Number ? ((Number) row[0]).longValue() : 0L;
            long qty = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            Map<String, Object> m = new HashMap<>();
            m.put("product_id", pid);
            m.put("quantity_sold", qty);
            productRepository
                    .findById(pid)
                    .ifPresent(p -> {
                        m.put("title", LocalizedTitleJson.storedToDisplayString(p.getTitle()));
                        m.put("slug", p.getSlug());
                    });
            items.add(m);
        }
        return Map.of("limit", RANKING_LIMIT, "items", items);
    }

    /** 当前无库存字段，占位返回空列表，便于前端对接。 */
    @Transactional(readOnly = true)
    public Map<String, Object> inventoryAlerts() {
        return Map.of(
                "items", List.of(),
                "note", "no_stock_field_in_product_model");
    }
}
