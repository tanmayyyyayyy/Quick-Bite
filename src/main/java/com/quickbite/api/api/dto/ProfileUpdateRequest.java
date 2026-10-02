package com.quickbite.api.api.dto;

import com.quickbite.api.service.command.ProfileUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Size(max = 32) String phone) {
    public ProfileUpdateCommand toCommand() {
        return new ProfileUpdateCommand(firstName, lastName, phone);
    }
}