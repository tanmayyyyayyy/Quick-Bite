package com.quickbite.api.service.command;

import java.math.BigDecimal;

public record MenuItemCommand(
        String name,
        String description,
        String category,
        BigDecimal price,
        boolean available) {
}