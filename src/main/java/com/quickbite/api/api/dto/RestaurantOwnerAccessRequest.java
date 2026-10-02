package com.quickbite.api.api.dto;

import jakarta.validation.constraints.NotNull;

public record RestaurantOwnerAccessRequest(@NotNull Boolean enabled) {
}
