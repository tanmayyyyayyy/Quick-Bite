package com.quickbite.api.api.dto;

import com.quickbite.api.service.command.RestaurantCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestaurantRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 80) String cuisine,
        @Size(max = 32) @Pattern(regexp = "^\\+?[0-9() -]{7,32}$") String phone,
        @NotBlank @Size(max = 160) String addressLine1,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String region,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(min = 2, max = 2) @Pattern(regexp = "[A-Za-z]{2}") String country) {
    public RestaurantCommand toCommand() {
        return new RestaurantCommand(name, description, cuisine, phone, addressLine1,
                city, region, postalCode, country);
    }
}