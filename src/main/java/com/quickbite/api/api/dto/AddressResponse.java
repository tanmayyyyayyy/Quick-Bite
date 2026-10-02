package com.quickbite.api.api.dto;

public record AddressResponse(Long id, String label, String recipientName, String phone,
        String addressLine1, String addressLine2, String city, String region,
        String postalCode, String country, boolean defaultAddress) {
}