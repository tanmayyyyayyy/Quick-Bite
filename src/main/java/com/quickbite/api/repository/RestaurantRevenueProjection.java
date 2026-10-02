package com.quickbite.api.repository;

import java.math.BigDecimal;

public interface RestaurantRevenueProjection {
    Long getRestaurantId();

    String getRestaurantName();

    BigDecimal getRevenue();

    Long getOrderCount();
}