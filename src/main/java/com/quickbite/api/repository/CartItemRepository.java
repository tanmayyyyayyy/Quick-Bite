package com.quickbite.api.repository;

import com.quickbite.api.entity.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

    boolean existsByCartIdAndMenuItemId(Long cartId, Long menuItemId);
}