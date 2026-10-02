package com.quickbite.api.repository;

import com.quickbite.api.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    @Query("""
            select item from MenuItem item
            where item.restaurant.id = :restaurantId
              and (:category is null or lower(item.category) = lower(:category))
              and (:search is null or lower(item.name) like lower(concat('%', :search, '%')))
              and (:available is null or item.available = :available)
            """)
    Page<MenuItem> searchMenu(
            @Param("restaurantId") Long restaurantId,
            @Param("category") String category,
            @Param("search") String search,
            @Param("available") Boolean available,
            Pageable pageable);

    Page<MenuItem> findByRestaurantIdAndAvailableTrue(Long restaurantId, Pageable pageable);
}