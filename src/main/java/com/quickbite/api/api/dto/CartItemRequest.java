package com.quickbite.api.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(@NotNull @Positive Long menuItemId, @Positive @Max(99) int quantity) {
}