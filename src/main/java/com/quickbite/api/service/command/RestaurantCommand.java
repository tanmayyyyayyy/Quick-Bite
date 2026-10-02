package com.quickbite.api.service.command;

public record RestaurantCommand(
        String name,
        String description,
        String cuisine,
        String phone,
        String addressLine1,
        String city,
        String region,
        String postalCode,
        String country) {
}