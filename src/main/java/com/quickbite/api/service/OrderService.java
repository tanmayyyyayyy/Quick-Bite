package com.quickbite.api.service;

import com.quickbite.api.entity.Address;
import com.quickbite.api.entity.Cart;
import com.quickbite.api.entity.CartItem;
import com.quickbite.api.entity.CustomerOrder;
import com.quickbite.api.entity.CustomerOrderItem;
import com.quickbite.api.entity.MenuItem;
import com.quickbite.api.entity.OrderStatus;
import com.quickbite.api.entity.Restaurant;
import com.quickbite.api.entity.User;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.BadRequestException;
import com.quickbite.api.exception.ForbiddenException;
import com.quickbite.api.exception.ResourceNotFoundException;
import com.quickbite.api.repository.AddressRepository;
import com.quickbite.api.repository.OrderRepository;
import com.quickbite.api.repository.RestaurantRepository;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.CreateOrderCommand;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final RestaurantRepository restaurantRepository;
    private final CartService cartService;
    private final BigDecimal deliveryFee;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            AddressRepository addressRepository,
            RestaurantRepository restaurantRepository,
            CartService cartService,
            @Value("${quickbite.order.delivery-fee:2.50}") BigDecimal deliveryFee) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.restaurantRepository = restaurantRepository;
        this.cartService = cartService;
        if (deliveryFee.signum() < 0) {
            throw new IllegalArgumentException("Delivery fee must not be negative");
        }
        this.deliveryFee = deliveryFee;
    }

    @Transactional
    public CustomerOrder create(Long userId, CreateOrderCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Address address = addressRepository.findByIdAndUserId(command.addressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", command.addressId()));
        Cart cart = cartService.getOrCreate(userId);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot create an order from an empty cart");
        }

        CartItem firstItem = cart.getItems().getFirst();
        Restaurant restaurant = firstItem.getMenuItem().getRestaurant();
        if (!restaurant.isActive()) {
            throw new BadRequestException("The restaurant is not accepting orders");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            MenuItem menuItem = cartItem.getMenuItem();
            if (cartItem.getQuantity() < 1 || cartItem.getQuantity() > 99) {
                throw new BadRequestException("Cart contains an invalid quantity");
            }
            if (!menuItem.isAvailable()) {
                throw new BadRequestException("Menu item is no longer available: " + menuItem.getName());
            }
            if (!menuItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new BadRequestException("A cart may only contain items from one restaurant");
            }
            subtotal = subtotal.add(menuItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setAddress(address);
        order.setStatus(OrderStatus.PLACED);
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(subtotal.add(deliveryFee));
        for (CartItem cartItem : cart.getItems()) {
            CustomerOrderItem orderItem = new CustomerOrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(cartItem.getMenuItem());
            orderItem.setItemName(cartItem.getMenuItem().getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getMenuItem().getPrice());
            order.getItems().add(orderItem);
        }

        CustomerOrder savedOrder = orderRepository.save(order);
        cartService.clear(cart);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrder> listForCustomer(Long userId, OrderStatus status, LocalDateTime startAt,
            LocalDateTime endAt, Pageable pageable) {
        return orderRepository.searchOrders(userId, null, status, startAt, endAt, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrder> listForRestaurantOwner(Long ownerId, Long restaurantId, OrderStatus status,
            LocalDateTime startAt, LocalDateTime endAt, Pageable pageable) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        if (!restaurant.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this restaurant");
        }
        return orderRepository.searchOrders(null, restaurantId, status, startAt, endAt, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrder> listAll(OrderStatus status, LocalDateTime startAt, LocalDateTime endAt, Pageable pageable) {
        return orderRepository.searchOrders(null, null, status, startAt, endAt, pageable);
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(Long orderId, Long actorId, UserRole role) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        boolean allowed = role == UserRole.ADMIN
                || order.getUser().getId().equals(actorId)
                || (role == UserRole.RESTAURANT_OWNER && order.getRestaurant().getOwner().getId().equals(actorId));
        if (!allowed) {
            throw new ForbiddenException("You may not access this order");
        }
        return order;
    }

    @Transactional
    public CustomerOrder changeStatus(Long orderId, Long actorId, UserRole role, OrderStatus nextStatus) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (role != UserRole.ADMIN
                && (role != UserRole.RESTAURANT_OWNER || !order.getRestaurant().getOwner().getId().equals(actorId))) {
            throw new ForbiddenException("Only the restaurant owner or an administrator may update order status");
        }
        if (!isAllowedTransition(order.getStatus(), nextStatus)) {
            throw new BadRequestException("Invalid order status transition: " + order.getStatus() + " to " + nextStatus);
        }
        order.setStatus(nextStatus);
        return order;
    }

    @Transactional
    public CustomerOrder cancel(Long orderId, Long actorId, UserRole role) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (role != UserRole.ADMIN && !order.getUser().getId().equals(actorId)) {
            throw new ForbiddenException("You may not cancel this order");
        }
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException("Only newly placed orders may be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }

    private static boolean isAllowedTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PLACED -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PREPARING;
            case PREPARING -> next == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}