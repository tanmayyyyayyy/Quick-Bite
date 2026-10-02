package com.quickbite.api.api.dto;

import java.math.BigDecimal;

public record MenuItemResponse(Long id, Long restaurantId, String name, String description,
        String category, BigDecimal price, boolean available) {
}