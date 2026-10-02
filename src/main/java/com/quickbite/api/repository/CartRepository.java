package com.quickbite.api.repository;

import com.quickbite.api.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"items", "items.menuItem", "items.menuItem.restaurant"})
    Optional<Cart> findWithItemsByUserId(Long userId);
}