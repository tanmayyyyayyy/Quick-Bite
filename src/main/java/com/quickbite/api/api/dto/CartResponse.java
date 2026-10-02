package com.quickbite.api.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long id, List<CartItemResponse> items, BigDecimal subtotal) {
}