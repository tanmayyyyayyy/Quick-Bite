package com.quickbite.api.api;

import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.api.dto.CartItemRequest;
import com.quickbite.api.api.dto.CartResponse;
import com.quickbite.api.api.dto.UpdateCartItemRequest;
import com.quickbite.api.entity.Cart;
import com.quickbite.api.entity.User;
import com.quickbite.api.service.CartService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {
    private final CartService cartService;
    private final ActorResolver actorResolver;
    private final ApiMapper apiMapper;

    public CartController(CartService cartService, ActorResolver actorResolver, ApiMapper apiMapper) {
        this.cartService = cartService;
        this.actorResolver = actorResolver;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal UserDetails principal) {
        User actor = actorResolver.requireUser(principal);
        Cart cart = cartService.getOrCreate(actor.getId());
        return apiMapper.toResponse(cart, cartService.calculateSubtotal(cart));
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CartItemRequest request) {
        User actor = actorResolver.requireUser(principal);
        Cart cart = cartService.addItem(actor.getId(),
                new com.quickbite.api.service.command.AddCartItemCommand(request.menuItemId(), request.quantity()));
        return apiMapper.toResponse(cart, cartService.calculateSubtotal(cart));
    }

    @PutMapping("/items/{itemId}")
    public CartResponse update(@PathVariable Long itemId, @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateCartItemRequest request) {
        User actor = actorResolver.requireUser(principal);
        Cart cart = cartService.updateQuantity(actor.getId(), itemId, request.quantity());
        return apiMapper.toResponse(cart, cartService.calculateSubtotal(cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> delete(@PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails principal) {
        User actor = actorResolver.requireUser(principal);
        Cart cart = cartService.removeItem(actor.getId(), itemId);
        return ResponseEntity.ok(apiMapper.toResponse(cart, cartService.calculateSubtotal(cart)));
    }
}
