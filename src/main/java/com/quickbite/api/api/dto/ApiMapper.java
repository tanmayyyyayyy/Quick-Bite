package com.quickbite.api.api.dto;

import com.quickbite.api.entity.Address;
import com.quickbite.api.entity.Cart;
import com.quickbite.api.entity.CustomerOrder;
import com.quickbite.api.entity.CustomerOrderItem;
import com.quickbite.api.entity.MenuItem;
import com.quickbite.api.entity.Restaurant;
import com.quickbite.api.entity.User;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getRole(), user.getCreatedAt());
    }

    public RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(restaurant.getId(), restaurant.getOwner().getId(), restaurant.getName(),
                restaurant.getDescription(), restaurant.getCuisine(), restaurant.getPhone(), restaurant.getAddressLine1(),
                restaurant.getCity(), restaurant.getRegion(), restaurant.getPostalCode(), restaurant.getCountry(),
                restaurant.getRating(), restaurant.isActive(), restaurant.getCreatedAt());
    }

    public MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getRestaurant().getId(), item.getName(), item.getDescription(),
                item.getCategory(), item.getPrice(), item.isAvailable());
    }

    public AddressResponse toResponse(Address address) {
        return new AddressResponse(address.getId(), address.getLabel(), address.getRecipientName(), address.getPhone(),
                address.getAddressLine1(), address.getAddressLine2(), address.getCity(), address.getRegion(),
                address.getPostalCode(), address.getCountry(), address.isDefaultAddress());
    }

    public CartResponse toResponse(Cart cart, BigDecimal subtotal) {
        List<CartItemResponse> items = cart.getItems().stream().map(item -> {
            MenuItem menuItem = item.getMenuItem();
            BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemResponse(item.getId(), menuItem.getId(), menuItem.getName(), menuItem.getCategory(),
                    item.getQuantity(), menuItem.getPrice(), lineTotal);
        }).toList();
        return new CartResponse(cart.getId(), items, subtotal);
    }

    public OrderSummaryResponse toSummary(CustomerOrder order) {
        return new OrderSummaryResponse(order.getId(), order.getUser().getId(), order.getRestaurant().getId(),
                order.getStatus(), order.getTotalAmount(), order.getCreatedAt(), order.getPlacedAt());
    }

    public OrderResponse toResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream().map(this::toResponse).toList();
        return new OrderResponse(order.getId(), order.getUser().getId(), order.getRestaurant().getId(),
                order.getAddress().getId(), order.getStatus(), order.getSubtotal(), order.getDeliveryFee(),
                order.getTotalAmount(), order.getPlacedAt(), items);
    }

    private OrderItemResponse toResponse(CustomerOrderItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(item.getId(), item.getMenuItem().getId(), item.getItemName(),
                item.getQuantity(), item.getUnitPrice(), lineTotal);
    }
}