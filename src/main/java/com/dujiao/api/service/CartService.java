package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.CartItem;
import com.dujiao.api.domain.Product;
import com.dujiao.api.dto.cart.CartItemDto;
import com.dujiao.api.dto.cart.UpsertCartItemRequest;
import com.dujiao.api.repository.CartItemRepository;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.util.LocalizedTitleJson;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CartItemDto> getCart(long userId) {
        return cartItemRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<CartItemDto> upsert(long userId, UpsertCartItemRequest req) {
        Product p =
                productRepository
                        .findById(req.productId())
                        .filter(Product::isActive)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "product_not_found"));
        CartItem item =
                cartItemRepository
                        .findByUserIdAndProductId(userId, req.productId())
                        .orElseGet(
                                () -> {
                                    CartItem c = new CartItem();
                                    c.setUserId(userId);
                                    c.setProductId(req.productId());
                                    return c;
                                });
        item.setQuantity(req.quantity());
        cartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public void deleteItem(long userId, long productId) {
        cartItemRepository.deleteByUserIdAndProductId(userId, productId);
    }

    private CartItemDto toDto(CartItem c) {
        Product p =
                productRepository
                        .findById(c.getProductId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.INTERNAL, "product_missing"));
        BigDecimal line = p.getPriceAmount().multiply(BigDecimal.valueOf(c.getQuantity()));
        return new CartItemDto(
                c.getProductId(),
                LocalizedTitleJson.storedToDisplayString(p.getTitle()),
                p.getSlug(),
                c.getQuantity(),
                p.getPriceAmount(),
                line);
    }
}
