package com.quickbite.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.api.entity.Address;
import com.quickbite.api.entity.Cart;
import com.quickbite.api.entity.CartItem;
import com.quickbite.api.entity.CustomerOrder;
import com.quickbite.api.entity.MenuItem;
import com.quickbite.api.entity.OrderStatus;
import com.quickbite.api.entity.Restaurant;
import com.quickbite.api.entity.User;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.ConflictException;
import com.quickbite.api.exception.UnprocessableEntityException;
import com.quickbite.api.repository.AddressRepository;
import com.quickbite.api.repository.OrderRepository;
import com.quickbite.api.repository.RestaurantRepository;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.CreateOrderCommand;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private CartService cartService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, userRepository, addressRepository,
                restaurantRepository, cartService, new BigDecimal("2.50"));
    }

    @Test
    void createsPricedOrderAndClearsCart() {
        User user = mock(User.class);
        Address address = mock(Address.class);
        Restaurant restaurant = new Restaurant();
        restaurant.setActive(true);
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        menuItem.setName("Noodle bowl");
        menuItem.setPrice(new BigDecimal("8.25"));
        menuItem.setAvailable(true);
        CartItem cartItem = new CartItem();
        cartItem.setMenuItem(menuItem);
        cartItem.setQuantity(2);
        Cart cart = new Cart();
        cart.getItems().add(cartItem);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(address));
        when(cartService.getOrCreate(5L)).thenReturn(cart);
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerOrder order = orderService.create(5L, new CreateOrderCommand(10L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getSubtotal()).isEqualByComparingTo("16.50");
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("2.50");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("19.00");
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("8.25");
        verify(cartService).clear(cart);
    }

    @Test
    void emptyCartCannotCreateOrder() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(mock(User.class)));
        when(addressRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(mock(Address.class)));
        when(cartService.getOrCreate(5L)).thenReturn(new Cart());

        assertThatThrownBy(() -> orderService.create(5L, new CreateOrderCommand(10L)))
                .isInstanceOf(UnprocessableEntityException.class);

        verify(orderRepository, never()).save(any(CustomerOrder.class));
        verify(cartService, never()).clear(any(Cart.class));
    }

    @Test
    void customerMayCancelOnlyTheirPlacedOrder() {
        User customer = mock(User.class);
        when(customer.getId()).thenReturn(5L);
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getUser()).thenReturn(customer);
        when(order.getStatus()).thenReturn(OrderStatus.PLACED);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        orderService.cancel(20L, 5L, UserRole.CUSTOMER);

        verify(order).setStatus(OrderStatus.CANCELLED);
    }

    @Test
    void rejectsCancellationAfterOrderWasConfirmed() {
        User customer = mock(User.class);
        when(customer.getId()).thenReturn(5L);
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getUser()).thenReturn(customer);
        when(order.getStatus()).thenReturn(OrderStatus.CONFIRMED);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(20L, 5L, UserRole.CUSTOMER))
                .isInstanceOf(ConflictException.class);

        verify(order, never()).setStatus(any());
    }
}
