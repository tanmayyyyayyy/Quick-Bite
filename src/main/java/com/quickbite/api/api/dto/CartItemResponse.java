package com.quickbite.api.api.dto;

import java.math.BigDecimal;

public record CartItemResponse(Long id, Long menuItemId, String name, String category,
        int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
}