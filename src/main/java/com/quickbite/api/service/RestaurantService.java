package com.quickbite.api.service;

import com.quickbite.api.entity.Restaurant;
import com.quickbite.api.entity.User;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.ForbiddenException;
import com.quickbite.api.exception.ResourceNotFoundException;
import com.quickbite.api.repository.RestaurantRepository;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.RestaurantCommand;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Restaurant> search(String cuisine, BigDecimal minimumRating, String search, Pageable pageable) {
        return restaurantRepository.searchActive(clean(cuisine), minimumRating, clean(search), pageable);
    }

    @Transactional(readOnly = true)
    public Restaurant getActive(Long id) {
        return restaurantRepository.findById(id)
                .filter(Restaurant::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
    }

    @Transactional(readOnly = true)
    public Page<Restaurant> getOwned(Long ownerId, Pageable pageable) {
        return restaurantRepository.findByOwnerId(ownerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Restaurant> getAll(Pageable pageable) {
        return restaurantRepository.findAll(pageable);
    }

    @Transactional
    public Restaurant create(Long actorId, UserRole actorRole, RestaurantCommand command) {
        requireOwnerRole(actorRole);
        User owner = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorId));
        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        apply(restaurant, command);
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant update(Long restaurantId, Long actorId, UserRole actorRole, RestaurantCommand command) {
        Restaurant restaurant = findAndAuthorize(restaurantId, actorId, actorRole);
        apply(restaurant, command);
        return restaurant;
    }

    @Transactional
    public void deactivate(Long restaurantId, Long actorId, UserRole actorRole) {
        Restaurant restaurant = findAndAuthorize(restaurantId, actorId, actorRole);
        restaurant.setActive(false);
    }

    private Restaurant findAndAuthorize(Long restaurantId, Long actorId, UserRole actorRole) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        if (actorRole != UserRole.ADMIN
            && (actorRole != UserRole.RESTAURANT_OWNER || !restaurant.getOwner().getId().equals(actorId))) {
            throw new ForbiddenException("You do not own this restaurant");
        }
        return restaurant;
    }

    private static void requireOwnerRole(UserRole role) {
        if (role != UserRole.RESTAURANT_OWNER && role != UserRole.ADMIN) {
            throw new ForbiddenException("A restaurant owner role is required");
        }
    }

    private static void apply(Restaurant restaurant, RestaurantCommand command) {
        restaurant.setName(command.name().trim());
        restaurant.setDescription(command.description());
        restaurant.setCuisine(command.cuisine().trim());
        restaurant.setPhone(command.phone());
        restaurant.setAddressLine1(command.addressLine1().trim());
        restaurant.setCity(command.city().trim());
        restaurant.setRegion(command.region());
        restaurant.setPostalCode(command.postalCode().trim());
        restaurant.setCountry(command.country().trim().toUpperCase());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}