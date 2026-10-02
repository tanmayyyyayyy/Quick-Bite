package com.quickbite.api.service;

import com.quickbite.api.entity.Cart;
import com.quickbite.api.entity.CartItem;
import com.quickbite.api.entity.MenuItem;
import com.quickbite.api.entity.User;
import com.quickbite.api.exception.BadRequestException;
import com.quickbite.api.exception.ConflictException;
import com.quickbite.api.exception.ResourceNotFoundException;
import com.quickbite.api.repository.CartItemRepository;
import com.quickbite.api.repository.CartRepository;
import com.quickbite.api.repository.MenuItemRepository;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.AddCartItemCommand;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            MenuItemRepository menuItemRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Cart getOrCreate(Long userId) {
        return findOrCreate(userId);
    }

    private Cart findOrCreate(Long userId) {
        return cartRepository.findWithItemsByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public Cart addItem(Long userId, AddCartItemCommand command) {
        requireQuantity(command.quantity());
        MenuItem menuItem = menuItemRepository.findById(command.menuItemId())
                .filter(MenuItem::isAvailable)
                .orElseThrow(() -> new ResourceNotFoundException("Available menu item", command.menuItemId()));
        if (!menuItem.getRestaurant().isActive()) {
            throw new BadRequestException("The restaurant is not accepting orders");
        }

        Cart cart = findOrCreate(userId);
        if (!cart.getItems().isEmpty() && cart.getItems().stream()
            .anyMatch(item -> !sameRestaurant(item.getMenuItem(), menuItem))) {
            throw new ConflictException("A cart can contain items from only one restaurant");
        }

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getMenuItem().getId().equals(menuItem.getId()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setMenuItem(menuItem);
            item.setQuantity(command.quantity());
            cart.getItems().add(item);
        } else {
            requireQuantity(existing.getQuantity() + command.quantity());
            existing.setQuantity(existing.getQuantity() + command.quantity());
        }
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateQuantity(Long userId, Long itemId, int quantity) {
        requireQuantity(quantity);
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));
        item.setQuantity(quantity);
        return cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", userId));
    }

    @Transactional
    public Cart removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));
        Cart cart = item.getCart();
        cart.getItems().remove(item);
        return cartRepository.save(cart);
    }

    public BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getMenuItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void clear(Cart cart) {
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private static void requireQuantity(int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new BadRequestException("Quantity must be between 1 and 99");
        }
    }

    private static boolean sameRestaurant(MenuItem first, MenuItem second) {
        if (first.getRestaurant() == second.getRestaurant()) {
            return true;
        }
        Long firstId = first.getRestaurant().getId();
        Long secondId = second.getRestaurant().getId();
        return firstId != null && Objects.equals(firstId, secondId);
    }
}