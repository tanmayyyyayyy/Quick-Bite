package com.quickbite.api.api;

import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.api.dto.ProfileUpdateRequest;
import com.quickbite.api.api.dto.UserResponse;
import com.quickbite.api.entity.User;
import com.quickbite.api.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final ActorResolver actorResolver;
    private final UserService userService;
    private final ApiMapper apiMapper;

    public UserController(ActorResolver actorResolver, UserService userService, ApiMapper apiMapper) {
        this.actorResolver = actorResolver;
        this.userService = userService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public UserResponse getMe(@AuthenticationPrincipal UserDetails principal) {
        return apiMapper.toResponse(actorResolver.requireUser(principal));
    }

    @PutMapping
    public UserResponse updateMe(@AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        User actor = actorResolver.requireUser(principal);
        return apiMapper.toResponse(userService.updateProfile(actor.getId(), request.toCommand()));
    }
}
