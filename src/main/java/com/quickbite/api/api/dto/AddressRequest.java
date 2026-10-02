package com.quickbite.api.api.dto;

import com.quickbite.api.service.command.AddressCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 40) String label,
        @NotBlank @Size(max = 160) String recipientName,
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Size(max = 160) String addressLine1,
        @Size(max = 160) String addressLine2,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String region,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country,
        boolean defaultAddress) {
    public AddressCommand toCommand() {
        return new AddressCommand(label, recipientName, phone, addressLine1, addressLine2,
                city, region, postalCode, country, defaultAddress);
    }
}