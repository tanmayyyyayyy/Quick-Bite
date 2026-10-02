package com.quickbite.api.api;

import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.api.dto.PageResponse;
import com.quickbite.api.api.dto.RestaurantRequest;
import com.quickbite.api.api.dto.RestaurantResponse;
import com.quickbite.api.entity.User;
import com.quickbite.api.service.RestaurantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {
    private final RestaurantService restaurantService;
    private final ActorResolver actorResolver;
    private final ApiMapper apiMapper;

    public RestaurantController(RestaurantService restaurantService, ActorResolver actorResolver, ApiMapper apiMapper) {
        this.restaurantService = restaurantService;
        this.actorResolver = actorResolver;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public PageResponse<RestaurantResponse> list(
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal minimumRating,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponse.from(restaurantService.search(cuisine, minimumRating, search, pageable), apiMapper::toResponse);
    }

    @GetMapping("/{id}")
    public RestaurantResponse get(@PathVariable Long id) {
        return apiMapper.toResponse(restaurantService.getActive(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<RestaurantResponse> create(@AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody RestaurantRequest request) {
        User actor = actorResolver.requireUser(principal);
        RestaurantResponse response = apiMapper.toResponse(
                restaurantService.create(actor.getId(), actor.getRole(), request.toCommand()));
        return ResponseEntity.created(URI.create("/api/restaurants/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public RestaurantResponse update(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody RestaurantRequest request) {
        User actor = actorResolver.requireUser(principal);
        return apiMapper.toResponse(restaurantService.update(id, actor.getId(), actor.getRole(), request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User actor = actorResolver.requireUser(principal);
        restaurantService.deactivate(id, actor.getId(), actor.getRole());
        return ResponseEntity.noContent().build();
    }
}
