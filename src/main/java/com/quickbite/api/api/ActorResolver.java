package com.quickbite.api.api;

import com.quickbite.api.entity.User;
import com.quickbite.api.exception.UnauthorizedException;
import com.quickbite.api.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class ActorResolver {
    private final UserService userService;

    public ActorResolver(UserService userService) {
        this.userService = userService;
    }

    public User requireUser(UserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        return userService.getByEmail(principal.getUsername());
    }
}
