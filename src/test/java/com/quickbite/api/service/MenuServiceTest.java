package com.quickbite.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickbite.api.entity.MenuItem;
import com.quickbite.api.entity.Restaurant;
import com.quickbite.api.entity.User;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.ForbiddenException;
import com.quickbite.api.repository.MenuItemRepository;
import com.quickbite.api.repository.RestaurantRepository;
import com.quickbite.api.service.command.MenuItemCommand;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private RestaurantRepository restaurantRepository;

    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(menuItemRepository, restaurantRepository);
    }

    @Test
    void ownerCanCreateMenuItem() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(8L);
        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        when(restaurantRepository.findById(3L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem item = menuService.create(3L, 8L, UserRole.RESTAURANT_OWNER,
                new MenuItemCommand("Veg bowl", null, "Lunch", new BigDecimal("9.50"), true));

        assertThat(item.getRestaurant()).isSameAs(restaurant);
        assertThat(item.getName()).isEqualTo("Veg bowl");
        assertThat(item.getPrice()).isEqualByComparingTo("9.50");
    }

    @Test
    void customerCannotCreateMenuItemEvenIfRestaurantOwnerIdMatches() {
        User owner = mock(User.class);
        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        when(restaurantRepository.findById(3L)).thenReturn(Optional.of(restaurant));

        MenuItemCommand command = new MenuItemCommand("Veg bowl", null, "Lunch", BigDecimal.TEN, true);
        assertThatThrownBy(() -> menuService.create(3L, 8L, UserRole.CUSTOMER, command))
                .isInstanceOf(ForbiddenException.class);
    }
}
