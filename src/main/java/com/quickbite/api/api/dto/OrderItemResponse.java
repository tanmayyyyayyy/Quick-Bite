package com.quickbite.api.api.dto;

import java.math.BigDecimal;

public record OrderItemResponse(Long id, Long menuItemId, String itemName, int quantity,
        BigDecimal unitPrice, BigDecimal lineTotal) {
}