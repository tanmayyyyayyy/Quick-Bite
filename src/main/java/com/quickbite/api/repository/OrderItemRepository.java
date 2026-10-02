package com.quickbite.api.repository;

import com.quickbite.api.entity.CustomerOrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<CustomerOrderItem, Long> {
    List<CustomerOrderItem> findByOrderId(Long orderId);
}