package com.dujiao.api.service;

import com.dujiao.api.domain.OrderLine;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.dto.order.OrderDetailDto;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.util.LocalizedTitleJson;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMappingService {

    private final ProductRepository productRepository;

    public OrderMappingService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public OrderDetailDto toDetail(ShopOrder order) {
        List<OrderDetailDto.OrderLineDto> lines = new ArrayList<>();
        for (OrderLine line : order.getLines()) {
            String title =
                    productRepository
                            .findById(line.getProductId())
                            .map(p -> LocalizedTitleJson.storedToDisplayString(p.getTitle()))
                            .orElse("");
            lines.add(
                    new OrderDetailDto.OrderLineDto(
                            line.getProductId(),
                            title,
                            line.getQuantity(),
                            line.getUnitPrice(),
                            line.getLineTotal()));
        }
        return new OrderDetailDto(
                order.getId(),
                order.getOrderNo(),
                order.getGuestEmail(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                lines);
    }
}
