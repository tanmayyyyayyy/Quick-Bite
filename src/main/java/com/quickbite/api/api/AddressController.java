package com.quickbite.api.api;

import com.quickbite.api.api.dto.AddressRequest;
import com.quickbite.api.api.dto.AddressResponse;
import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.entity.User;
import com.quickbite.api.service.AddressService;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {
    private final AddressService addressService;
    private final ActorResolver actorResolver;
    private final ApiMapper apiMapper;

    public AddressController(AddressService addressService, ActorResolver actorResolver, ApiMapper apiMapper) {
        this.addressService = addressService;
        this.actorResolver = actorResolver;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public java.util.List<AddressResponse> list(@AuthenticationPrincipal UserDetails principal) {
        User actor = actorResolver.requireUser(principal);
        return addressService.getForUser(actor.getId()).stream().map(apiMapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody AddressRequest request) {
        User actor = actorResolver.requireUser(principal);
        AddressResponse response = apiMapper.toResponse(addressService.create(actor.getId(), request.toCommand()));
        return ResponseEntity.created(URI.create("/api/addresses/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody AddressRequest request) {
        User actor = actorResolver.requireUser(principal);
        return apiMapper.toResponse(addressService.update(actor.getId(), id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User actor = actorResolver.requireUser(principal);
        addressService.delete(actor.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
