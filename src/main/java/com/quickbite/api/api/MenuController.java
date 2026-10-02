package com.quickbite.api.api;

import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.api.dto.MenuItemRequest;
import com.quickbite.api.api.dto.MenuItemResponse;
import com.quickbite.api.api.dto.PageResponse;
import com.quickbite.api.entity.User;
import com.quickbite.api.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {
    private final MenuService menuService;
    private final ActorResolver actorResolver;
    private final ApiMapper apiMapper;

    public MenuController(MenuService menuService, ActorResolver actorResolver, ApiMapper apiMapper) {
        this.menuService = menuService;
        this.actorResolver = actorResolver;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/api/restaurants/{restaurantId}/menu")
    public PageResponse<MenuItemResponse> list(@PathVariable Long restaurantId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean available,
            @PageableDefault(size = 30, sort = "name") Pageable pageable) {
        return PageResponse.from(menuService.search(restaurantId, category, search, available, pageable), apiMapper::toResponse);
    }

    @PostMapping("/api/restaurants/{restaurantId}/menu")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<MenuItemResponse> create(@PathVariable Long restaurantId,
            @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody MenuItemRequest request) {
        User actor = actorResolver.requireUser(principal);
        MenuItemResponse response = apiMapper.toResponse(
                menuService.create(restaurantId, actor.getId(), actor.getRole(), request.toCommand()));
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/api/menu-items/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public MenuItemResponse update(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MenuItemRequest request) {
        User actor = actorResolver.requireUser(principal);
        return apiMapper.toResponse(menuService.update(id, actor.getId(), actor.getRole(), request.toCommand()));
    }

    @DeleteMapping("/api/menu-items/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User actor = actorResolver.requireUser(principal);
        menuService.deactivate(id, actor.getId(), actor.getRole());
        return ResponseEntity.noContent().build();
    }
}
