package com.quickbite.api.repository;

import com.quickbite.api.entity.Restaurant;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Page<Restaurant> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("""
            select restaurant from Restaurant restaurant
            where restaurant.active = true
              and (:cuisine is null or lower(restaurant.cuisine) = lower(:cuisine))
              and (:minimumRating is null or restaurant.rating >= :minimumRating)
              and (:search is null or lower(restaurant.name) like lower(concat('%', :search, '%')))
            """)
    Page<Restaurant> searchActive(
            @Param("cuisine") String cuisine,
            @Param("minimumRating") BigDecimal minimumRating,
            @Param("search") String search,
            Pageable pageable);
}