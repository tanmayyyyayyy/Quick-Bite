package com.quickbite.api.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String cuisine,
        String phone,
        String addressLine1,
        String city,
        String region,
        String postalCode,
        String country,
        BigDecimal rating,
        boolean active,
        LocalDateTime createdAt) {
}