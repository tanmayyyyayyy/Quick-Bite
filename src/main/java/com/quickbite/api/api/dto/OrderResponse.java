package com.quickbite.api.api.dto;

import com.quickbite.api.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id, Long userId, Long restaurantId, Long addressId,
        OrderStatus status, BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal totalAmount,
        LocalDateTime placedAt, List<OrderItemResponse> items) {
}