package com.quickbite.api.api.dto;

import com.quickbite.api.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.Map;

public record AdminStatisticsResponse(
        long userCount,
        long restaurantCount,
        long orderCount,
        BigDecimal revenue,
        Map<OrderStatus, Long> ordersByStatus) {
}