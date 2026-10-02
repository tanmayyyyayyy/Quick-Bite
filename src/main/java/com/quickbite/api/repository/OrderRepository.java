package com.quickbite.api.repository;

import com.quickbite.api.entity.CustomerOrder;
import com.quickbite.api.entity.OrderStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByUserId(Long userId, Pageable pageable);

    Page<CustomerOrder> findByRestaurantId(Long restaurantId, Pageable pageable);

    Page<CustomerOrder> findByStatus(OrderStatus status, Pageable pageable);

    @Query("""
            select customerOrder from CustomerOrder customerOrder
            where (:userId is null or customerOrder.user.id = :userId)
              and (:restaurantId is null or customerOrder.restaurant.id = :restaurantId)
              and (:status is null or customerOrder.status = :status)
              and (:startAt is null or customerOrder.createdAt >= :startAt)
              and (:endAt is null or customerOrder.createdAt < :endAt)
            """)
    Page<CustomerOrder> searchOrders(
            @Param("userId") Long userId,
            @Param("restaurantId") Long restaurantId,
            @Param("status") OrderStatus status,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            Pageable pageable);

    long countByStatus(OrderStatus status);

        @Query(value = """
            select restaurant.id as restaurantId,
                   restaurant.name as restaurantName,
                   sum(customerOrder.totalAmount) as revenue,
                   count(customerOrder.id) as orderCount
            from CustomerOrder customerOrder
            join customerOrder.restaurant restaurant
            where customerOrder.status <> :excludedStatus
            group by restaurant.id, restaurant.name
            order by sum(customerOrder.totalAmount) desc
            """,
            countQuery = """
                    select count(distinct restaurant.id)
                    from CustomerOrder customerOrder
                    join customerOrder.restaurant restaurant
                    where customerOrder.status <> :excludedStatus
                    """)
    Page<RestaurantRevenueProjection> findRestaurantRevenue(
            @Param("excludedStatus") OrderStatus excludedStatus,
            Pageable pageable);
}