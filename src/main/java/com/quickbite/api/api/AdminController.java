package com.quickbite.api.api;

import com.quickbite.api.api.dto.AdminStatisticsResponse;
import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.api.dto.OrderSummaryResponse;
import com.quickbite.api.api.dto.PageResponse;
import com.quickbite.api.api.dto.RestaurantResponse;
import com.quickbite.api.api.dto.RestaurantOwnerAccessRequest;
import com.quickbite.api.api.dto.UserResponse;
import com.quickbite.api.entity.OrderStatus;
import com.quickbite.api.service.AdminService;
import com.quickbite.api.service.OrderService;
import com.quickbite.api.service.RestaurantService;
import com.quickbite.api.service.UserService;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
    private final RestaurantService restaurantService;
    private final OrderService orderService;
    private final AdminService adminService;
    private final ApiMapper apiMapper;

    public AdminController(UserService userService, RestaurantService restaurantService, OrderService orderService,
            AdminService adminService, ApiMapper apiMapper) {
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.orderService = orderService;
        this.adminService = adminService;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageResponse.from(userService.searchUsers(search, pageable), apiMapper::toResponse);
    }

    @PatchMapping("/users/{id}/restaurant-owner")
    public UserResponse setRestaurantOwner(@PathVariable Long id,
            @Valid @RequestBody RestaurantOwnerAccessRequest request) {
        return apiMapper.toResponse(userService.setRestaurantOwnerAccess(id, request.enabled()));
    }

    @GetMapping("/restaurants")
    public PageResponse<RestaurantResponse> restaurants(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageResponse.from(restaurantService.getAll(pageable), apiMapper::toResponse);
    }

    @GetMapping("/orders")
    public PageResponse<OrderSummaryResponse> orders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.from(orderService.listAll(status, startAt, endAt, pageable), apiMapper::toSummary);
    }

    @GetMapping("/statistics")
    public AdminStatisticsResponse statistics() {
        return adminService.statistics();
    }
}
