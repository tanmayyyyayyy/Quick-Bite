package com.quickbite.api.api;

import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.api.dto.RegisterRequest;
import com.quickbite.api.api.dto.UserResponse;
import com.quickbite.api.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final ApiMapper apiMapper;

    public AuthController(UserService userService, ApiMapper apiMapper) {
        this.userService = userService;
        this.apiMapper = apiMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = apiMapper.toResponse(userService.register(request.email(), request.password(),
                request.firstName(), request.lastName(), request.phone()));
        return ResponseEntity.created(URI.create("/api/users/me")).body(response);
    }
}
