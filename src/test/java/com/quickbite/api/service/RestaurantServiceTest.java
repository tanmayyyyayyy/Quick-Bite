package com.quickbite.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickbite.api.entity.Restaurant;
import com.quickbite.api.entity.User;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.ForbiddenException;
import com.quickbite.api.repository.RestaurantRepository;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.RestaurantCommand;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private UserRepository userRepository;

    private RestaurantService restaurantService;

    @BeforeEach
    void setUp() {
        restaurantService = new RestaurantService(restaurantRepository, userRepository);
    }

    @Test
    void rejectsCustomerCreatingRestaurant() {
        RestaurantCommand request = command();
        assertThatThrownBy(() -> restaurantService.create(3L, UserRole.CUSTOMER, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsRestaurantUpdateByDifferentOwner() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(9L);
        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        when(restaurantRepository.findById(4L)).thenReturn(Optional.of(restaurant));

        RestaurantCommand request = command();
        assertThatThrownBy(() -> restaurantService.update(4L, 3L, UserRole.RESTAURANT_OWNER, request))
                .isInstanceOf(ForbiddenException.class);
    }

    private static RestaurantCommand command() {
        return new RestaurantCommand("Bowl House", null, "Asian", null,
                "1 Market Street", "Oakland", null, "94612", "US");
    }
}
