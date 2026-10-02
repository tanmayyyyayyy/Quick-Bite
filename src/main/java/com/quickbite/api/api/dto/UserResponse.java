package com.quickbite.api.api.dto;

import com.quickbite.api.entity.UserRole;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserRole role,
        LocalDateTime createdAt) {
}