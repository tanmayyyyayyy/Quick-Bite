package com.quickbite.api.api.dto;

import com.quickbite.api.service.command.MenuItemCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 80) String category,
        @NotNull @Positive BigDecimal price,
        Boolean available) {
    public MenuItemCommand toCommand() {
        return new MenuItemCommand(name, description, category, price, available == null || available);
    }
}