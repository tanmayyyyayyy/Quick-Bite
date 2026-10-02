package com.quickbite.api.service.command;

public record AddCartItemCommand(Long menuItemId, int quantity) {
}