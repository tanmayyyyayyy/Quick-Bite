package com.quickbite.api.service.command;

public record ProfileUpdateCommand(String firstName, String lastName, String phone) {
}