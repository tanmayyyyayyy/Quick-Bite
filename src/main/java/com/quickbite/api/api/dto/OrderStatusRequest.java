package com.quickbite.api.api.dto;

import com.quickbite.api.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull OrderStatus status) {
}