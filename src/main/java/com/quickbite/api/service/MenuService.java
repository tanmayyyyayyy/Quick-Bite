package com.quickbite.api.service;

import com.quickbite.api.entity.MenuItem;
import com.quickbite.api.entity.Restaurant;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.ForbiddenException;
import com.quickbite.api.exception.ResourceNotFoundException;
import com.quickbite.api.repository.MenuItemRepository;
import com.quickbite.api.repository.RestaurantRepository;
import com.quickbite.api.service.command.MenuItemCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuService(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public Page<MenuItem> search(Long restaurantId, String category, String search, Boolean available, Pageable pageable) {
        ensureRestaurantActive(restaurantId);
        return menuItemRepository.searchMenu(restaurantId, clean(category), clean(search), available, pageable);
    }

    @Transactional
    public MenuItem create(Long restaurantId, Long actorId, UserRole actorRole, MenuItemCommand command) {
        Restaurant restaurant = findAndAuthorizeRestaurant(restaurantId, actorId, actorRole);
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        apply(item, command);
        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem update(Long itemId, Long actorId, UserRole actorRole, MenuItemCommand command) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", itemId));
        authorize(item.getRestaurant(), actorId, actorRole);
        apply(item, command);
        return item;
    }

    @Transactional
    public void deactivate(Long itemId, Long actorId, UserRole actorRole) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", itemId));
        authorize(item.getRestaurant(), actorId, actorRole);
        item.setAvailable(false);
    }

    private void ensureRestaurantActive(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        if (!restaurant.isActive()) {
            throw new ResourceNotFoundException("Restaurant", id);
        }
    }

    private Restaurant findAndAuthorizeRestaurant(Long id, Long actorId, UserRole actorRole) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        authorize(restaurant, actorId, actorRole);
        return restaurant;
    }

    private static void authorize(Restaurant restaurant, Long actorId, UserRole actorRole) {
        if (actorRole != UserRole.ADMIN
            && (actorRole != UserRole.RESTAURANT_OWNER || !restaurant.getOwner().getId().equals(actorId))) {
            throw new ForbiddenException("You do not own this restaurant");
        }
    }

    private static void apply(MenuItem item, MenuItemCommand command) {
        item.setName(command.name().trim());
        item.setDescription(command.description());
        item.setCategory(command.category().trim());
        item.setPrice(command.price());
        item.setAvailable(command.available());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}