package com.quickbite.api.api.dto;

import com.quickbite.api.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(Long id, Long userId, Long restaurantId, OrderStatus status,
        BigDecimal totalAmount, LocalDateTime createdAt, LocalDateTime placedAt) {
}