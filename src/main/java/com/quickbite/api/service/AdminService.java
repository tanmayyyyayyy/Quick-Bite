package com.quickbite.api.service;

import com.quickbite.api.api.dto.AdminStatisticsResponse;
import com.quickbite.api.entity.OrderStatus;
import com.quickbite.api.repository.OrderRepository;
import com.quickbite.api.repository.RestaurantRepository;
import com.quickbite.api.repository.UserRepository;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;

    public AdminService(UserRepository userRepository, RestaurantRepository restaurantRepository,
            OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatisticsResponse statistics() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, orderRepository.countByStatus(status));
        }
        BigDecimal revenue = orderRepository.calculateRevenueExcludingStatus(OrderStatus.CANCELLED);
        return new AdminStatisticsResponse(userRepository.count(), restaurantRepository.count(),
                orderRepository.count(), revenue, Map.copyOf(counts));
    }
}