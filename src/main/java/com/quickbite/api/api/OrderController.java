package com.quickbite.api.api;

import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.api.dto.CreateOrderRequest;
import com.quickbite.api.api.dto.OrderResponse;
import com.quickbite.api.api.dto.OrderStatusRequest;
import com.quickbite.api.api.dto.OrderSummaryResponse;
import com.quickbite.api.api.dto.PageResponse;
import com.quickbite.api.entity.CustomerOrder;
import com.quickbite.api.entity.OrderStatus;
import com.quickbite.api.entity.User;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.BadRequestException;
import com.quickbite.api.service.OrderService;
import com.quickbite.api.service.command.CreateOrderCommand;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final ActorResolver actorResolver;
    private final ApiMapper apiMapper;

    public OrderController(OrderService orderService, ActorResolver actorResolver, ApiMapper apiMapper) {
        this.orderService = orderService;
        this.actorResolver = actorResolver;
        this.apiMapper = apiMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateOrderRequest request) {
        User actor = actorResolver.requireUser(principal);
        CustomerOrder order = orderService.create(actor.getId(), new CreateOrderCommand(request.addressId()));
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                .body(apiMapper.toResponse(order));
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> list(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        validateDateRange(startAt, endAt);
        User actor = actorResolver.requireUser(principal);
        var orders = switch (actor.getRole()) {
            case CUSTOMER -> orderService.listForCustomer(actor.getId(), status, startAt, endAt, pageable);
            case RESTAURANT_OWNER -> {
                if (restaurantId == null) {
                    throw new BadRequestException("restaurantId is required for restaurant owners");
                }
                yield orderService.listForRestaurantOwner(actor.getId(), restaurantId, status, startAt, endAt, pageable);
            }
            case ADMIN -> orderService.listAll(status, startAt, endAt, pageable);
        };
        return PageResponse.from(orders, apiMapper::toSummary);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User actor = actorResolver.requireUser(principal);
        return apiMapper.toResponse(orderService.get(id, actor.getId(), actor.getRole()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public OrderSummaryResponse updateStatus(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody OrderStatusRequest request) {
        User actor = actorResolver.requireUser(principal);
        return apiMapper.toSummary(orderService.changeStatus(id, actor.getId(), actor.getRole(), request.status()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public OrderSummaryResponse cancel(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User actor = actorResolver.requireUser(principal);
        return apiMapper.toSummary(orderService.cancel(id, actor.getId(), actor.getRole()));
    }

    private static void validateDateRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw new BadRequestException("startAt must be before endAt");
        }
    }
}
