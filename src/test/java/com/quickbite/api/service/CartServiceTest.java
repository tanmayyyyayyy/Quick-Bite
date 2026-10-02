package com.quickbite.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.api.entity.Cart;
import com.quickbite.api.entity.CartItem;
import com.quickbite.api.entity.MenuItem;
import com.quickbite.api.entity.Restaurant;
import com.quickbite.api.exception.ConflictException;
import com.quickbite.api.repository.CartItemRepository;
import com.quickbite.api.repository.CartRepository;
import com.quickbite.api.repository.MenuItemRepository;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.AddCartItemCommand;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private UserRepository userRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, menuItemRepository, userRepository);
    }

    @Test
    void addsAvailableItemAndMergesSubtotalQuantity() {
        Restaurant restaurant = new Restaurant();
        restaurant.setActive(true);
        MenuItem menuItem = new MenuItem();
        menuItem.setName("Noodle bowl");
        menuItem.setPrice(new BigDecimal("8.25"));
        menuItem.setAvailable(true);
        menuItem.setRestaurant(restaurant);
        Cart cart = new Cart();
        when(menuItemRepository.findById(15L)).thenReturn(Optional.of(menuItem));
        when(cartRepository.findWithItemsByUserId(5L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart saved = cartService.addItem(5L, new AddCartItemCommand(15L, 2));

        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().getFirst().getQuantity()).isEqualTo(2);
        assertThat(cartService.calculateSubtotal(saved)).isEqualByComparingTo("16.50");
    }

    @Test
    void refusesItemsFromAnotherRestaurant() {
        Restaurant currentRestaurant = new Restaurant();
        currentRestaurant.setActive(true);
        Restaurant secondRestaurant = new Restaurant();
        secondRestaurant.setActive(true);
        MenuItem currentItem = new MenuItem();
        currentItem.setRestaurant(currentRestaurant);
        MenuItem requestedItem = new MenuItem();
        requestedItem.setRestaurant(secondRestaurant);
        requestedItem.setAvailable(true);
        CartItem cartItem = new CartItem();
        cartItem.setMenuItem(currentItem);
        Cart cart = new Cart();
        cart.getItems().add(cartItem);
        when(menuItemRepository.findById(16L)).thenReturn(Optional.of(requestedItem));
        when(cartRepository.findWithItemsByUserId(5L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(5L, new AddCartItemCommand(16L, 1)))
                .isInstanceOf(ConflictException.class);

        verify(cartRepository, never()).save(any(Cart.class));
    }
}
