package com.quickbite.api.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(@NotNull @Positive Long addressId) {
}