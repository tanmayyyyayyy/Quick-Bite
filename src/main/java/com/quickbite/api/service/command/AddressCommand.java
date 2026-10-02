package com.quickbite.api.service.command;

public record AddressCommand(
        String label,
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String country,
        boolean defaultAddress) {
}